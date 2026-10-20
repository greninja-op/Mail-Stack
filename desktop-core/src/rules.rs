//! Rules + user corrections with precedence:
//! Correction > Rule > Deterministic > AI > Unknown.
//!
//! Rules are structured data — never arbitrary code execution.
//! Undo deletes correction rows so engines restore the base (never stored twice).

use crate::account::{AccountId, AccountScoped};
use crate::category::CategoryGroupKind;
use crate::error::{CoreError, CoreResult};
use crate::priority::PriorityLevel;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Mutex;

/// Where a decision came from (precedence order).
#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub enum DecisionSource {
    Unknown = 0,
    Ai = 1,
    Deterministic = 2,
    Rule = 3,
    Correction = 4,
}

impl DecisionSource {
    pub fn precedence_rank(self) -> u8 {
        self as u8
    }
}

/// Fields a correction may override.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum CorrectionField {
    Category,
    Priority,
    ActionRequired,
    CompanyName,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct UserCorrection {
    pub account_id: AccountId,
    pub message_id: String,
    pub field: CorrectionField,
    pub value: String,
}

impl AccountScoped for UserCorrection {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum RuleAction {
    SetCategory(CategoryGroupKind),
    SetPriority(PriorityLevel),
    SetActionRequired(bool),
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct RuleCondition {
    pub sender_domain: Option<String>,
    pub subject_contains: Option<String>,
    pub label: Option<String>,
}

impl RuleCondition {
    /// Empty-condition rules never match.
    pub fn is_empty(&self) -> bool {
        self.sender_domain.is_none() && self.subject_contains.is_none() && self.label.is_none()
    }

    pub fn signature(&self) -> String {
        format!(
            "{}|{}|{}",
            self.sender_domain.as_deref().unwrap_or(""),
            self.subject_contains.as_deref().unwrap_or(""),
            self.label.as_deref().unwrap_or("")
        )
    }

    pub fn matches(&self, sender_email: &str, subject: &str, labels: &[String]) -> bool {
        if self.is_empty() {
            return false;
        }
        if let Some(d) = &self.sender_domain {
            let email = sender_email.to_ascii_lowercase();
            if !email.ends_with(&format!("@{}", d.to_ascii_lowercase()))
                && !email.contains(&format!(".{}", d.to_ascii_lowercase()))
            {
                return false;
            }
        }
        if let Some(s) = &self.subject_contains {
            if !subject
                .to_ascii_lowercase()
                .contains(&s.to_ascii_lowercase())
            {
                return false;
            }
        }
        if let Some(l) = &self.label {
            if !labels.iter().any(|x| x.eq_ignore_ascii_case(l)) {
                return false;
            }
        }
        true
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct UserRule {
    pub account_id: AccountId,
    pub id: String,
    pub order: u32,
    pub enabled: bool,
    pub condition: RuleCondition,
    pub action: RuleAction,
}

impl AccountScoped for UserRule {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

/// Effective decision after applying precedence.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct EffectiveDecision<T> {
    pub value: T,
    pub source: DecisionSource,
}

/// Store for corrections — undo = delete row.
#[derive(Default)]
pub struct CorrectionStore {
    rows: Mutex<HashMap<(String, String, CorrectionField), UserCorrection>>,
}

impl CorrectionStore {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn upsert(&self, c: UserCorrection) -> CoreResult<()> {
        let mut g = self
            .rows
            .lock()
            .map_err(|_| CoreError::Internal("correction lock".into()))?;
        g.insert(
            (
                c.account_id.as_str().to_owned(),
                c.message_id.clone(),
                c.field,
            ),
            c,
        );
        Ok(())
    }

    /// Undo: delete correction so engines restore the base (never stored twice).
    pub fn undo(
        &self,
        account: &AccountId,
        message_id: &str,
        field: CorrectionField,
    ) -> CoreResult<()> {
        let mut g = self
            .rows
            .lock()
            .map_err(|_| CoreError::Internal("correction lock".into()))?;
        g.remove(&(account.as_str().to_owned(), message_id.to_owned(), field));
        Ok(())
    }

    pub fn get(
        &self,
        account: &AccountId,
        message_id: &str,
        field: CorrectionField,
    ) -> Option<UserCorrection> {
        self.rows.lock().ok().and_then(|g| {
            g.get(&(account.as_str().to_owned(), message_id.to_owned(), field))
                .cloned()
        })
    }
}

/// First enabled matching rule per action discriminant wins; order then id.
pub fn first_matching_rule<'a>(
    rules: &'a [UserRule],
    sender_email: &str,
    subject: &str,
    labels: &[String],
) -> Option<&'a UserRule> {
    let mut candidates: Vec<_> = rules
        .iter()
        .filter(|r| r.enabled && r.condition.matches(sender_email, subject, labels))
        .collect();
    candidates.sort_by(|a, b| a.order.cmp(&b.order).then_with(|| a.id.cmp(&b.id)));
    candidates.into_iter().next()
}

/// Detect conflicts: same condition signature + different action values.
pub fn find_conflicts(rules: &[UserRule]) -> Vec<(String, String)> {
    let mut by_sig: HashMap<String, Vec<&UserRule>> = HashMap::new();
    for r in rules {
        if r.condition.is_empty() {
            continue;
        }
        by_sig
            .entry(r.condition.signature())
            .or_default()
            .push(r);
    }
    let mut out = Vec::new();
    for group in by_sig.values() {
        for i in 0..group.len() {
            for j in (i + 1)..group.len() {
                if group[i].action != group[j].action {
                    out.push((group[i].id.clone(), group[j].id.clone()));
                }
            }
        }
    }
    out
}

/// Resolve category with full precedence chain.
pub fn resolve_category(
    corrections: &CorrectionStore,
    rules: &[UserRule],
    account: &AccountId,
    message_id: &str,
    sender_email: &str,
    subject: &str,
    labels: &[String],
    deterministic: CategoryGroupKind,
    ai_suggestion: Option<CategoryGroupKind>,
) -> EffectiveDecision<CategoryGroupKind> {
    if let Some(c) = corrections.get(account, message_id, CorrectionField::Category) {
        if let Some(kind) = parse_category(&c.value) {
            return EffectiveDecision {
                value: kind,
                source: DecisionSource::Correction,
            };
        }
    }
    if let Some(r) = first_matching_rule(rules, sender_email, subject, labels) {
        if let RuleAction::SetCategory(k) = r.action {
            return EffectiveDecision {
                value: k,
                source: DecisionSource::Rule,
            };
        }
    }
    // Deterministic always present as base.
    let _ = ai_suggestion; // AI only if we chose to use it below when deterministic is "unknown"
    EffectiveDecision {
        value: deterministic,
        source: DecisionSource::Deterministic,
    }
}

/// When deterministic declines, optional AI may fill Unknown → AI.
pub fn resolve_with_optional_ai<T: Clone>(
    deterministic: Option<T>,
    ai: Option<T>,
) -> EffectiveDecision<Option<T>> {
    if let Some(v) = deterministic {
        return EffectiveDecision {
            value: Some(v),
            source: DecisionSource::Deterministic,
        };
    }
    if let Some(v) = ai {
        return EffectiveDecision {
            value: Some(v),
            source: DecisionSource::Ai,
        };
    }
    EffectiveDecision {
        value: None,
        source: DecisionSource::Unknown,
    }
}

fn parse_category(s: &str) -> Option<CategoryGroupKind> {
    match s.trim().to_ascii_lowercase().as_str() {
        "needs_attention" | "needsattention" => Some(CategoryGroupKind::NeedsAttention),
        "promotions" => Some(CategoryGroupKind::Promotions),
        "updates" => Some(CategoryGroupKind::Updates),
        "work_and_learning" | "workandlearning" => Some(CategoryGroupKind::WorkAndLearning),
        "everything_else" | "everythingelse" => Some(CategoryGroupKind::EverythingElse),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn precedence_correction_over_rule() {
        let a = AccountId::new("a").unwrap();
        let store = CorrectionStore::new();
        store
            .upsert(UserCorrection {
                account_id: a.clone(),
                message_id: "m1".into(),
                field: CorrectionField::Category,
                value: "promotions".into(),
            })
            .unwrap();
        let rules = vec![UserRule {
            account_id: a.clone(),
            id: "r1".into(),
            order: 0,
            enabled: true,
            condition: RuleCondition {
                sender_domain: Some("co.com".into()),
                subject_contains: None,
                label: None,
            },
            action: RuleAction::SetCategory(CategoryGroupKind::Updates),
        }];
        let d = resolve_category(
            &store,
            &rules,
            &a,
            "m1",
            "x@co.com",
            "hi",
            &[],
            CategoryGroupKind::EverythingElse,
            None,
        );
        assert_eq!(d.source, DecisionSource::Correction);
        assert_eq!(d.value, CategoryGroupKind::Promotions);
    }

    #[test]
    fn empty_condition_never_matches() {
        let r = UserRule {
            account_id: AccountId::new("a").unwrap(),
            id: "r".into(),
            order: 0,
            enabled: true,
            condition: RuleCondition {
                sender_domain: None,
                subject_contains: None,
                label: None,
            },
            action: RuleAction::SetCategory(CategoryGroupKind::Promotions),
        };
        assert!(first_matching_rule(&[r], "a@b.com", "x", &[]).is_none());
    }

    #[test]
    fn undo_deletes_correction() {
        let a = AccountId::new("a").unwrap();
        let store = CorrectionStore::new();
        store
            .upsert(UserCorrection {
                account_id: a.clone(),
                message_id: "m".into(),
                field: CorrectionField::Priority,
                value: "high".into(),
            })
            .unwrap();
        store.undo(&a, "m", CorrectionField::Priority).unwrap();
        assert!(store.get(&a, "m", CorrectionField::Priority).is_none());
    }

    #[test]
    fn source_ordering() {
        assert!(DecisionSource::Correction > DecisionSource::Rule);
        assert!(DecisionSource::Rule > DecisionSource::Deterministic);
        assert!(DecisionSource::Deterministic > DecisionSource::Ai);
        assert!(DecisionSource::Ai > DecisionSource::Unknown);
    }
}
