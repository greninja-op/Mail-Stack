//! Advanced automation (phase 25) — user-created rules only; finite & explainable.
//!
//! Email content cannot create or approve an automation. Circuit breaker on failures.

use crate::account::{AccountId, AccountScoped};
use crate::error::{CoreError, CoreResult};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Mutex;

pub const CIRCUIT_BREAKER_THRESHOLD: u32 = 5;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AutomationTrigger {
    MessageReceived,
    CategoryAssigned,
    ActionRequiredSet,
    TemporalExtracted,
    Manual,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AutomationActionKind {
    ApplyLabel,
    SetCategory,
    CreateTaskProposal,
    Notify,
    ArchiveProposal,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct AutomationRule {
    pub account_id: AccountId,
    pub id: String,
    pub name: String,
    pub enabled: bool,
    pub trigger: AutomationTrigger,
    pub condition_summary: String,
    pub action: AutomationActionKind,
    /// Destructive actions require explicit confirmation.
    pub destructive: bool,
}

impl AccountScoped for AutomationRule {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct AutomationProposal {
    pub rule_id: String,
    pub account_id: AccountId,
    pub preview: String,
    pub requires_confirmation: bool,
}

/// Engine proposes; user confirms; nothing external automatic.
#[derive(Default)]
pub struct AutomationEngine {
    failure_counts: Mutex<HashMap<String, u32>>,
    open_circuits: Mutex<HashMap<String, bool>>,
}

impl AutomationEngine {
    pub fn new() -> Self {
        Self::default()
    }

    /// Email/AI content must never create rules — only explicit user input.
    pub fn create_rule_from_user(&self, rule: AutomationRule) -> CoreResult<AutomationRule> {
        if rule.name.trim().is_empty() {
            return Err(CoreError::InvalidArgument("automation name required".into()));
        }
        Ok(rule)
    }

    /// Propose an action for `target_account`. Cross-account execution is rejected.
    pub fn propose(
        &self,
        rule: &AutomationRule,
        target_account: &AccountId,
        context_preview: &str,
    ) -> CoreResult<AutomationProposal> {
        if !rule.enabled {
            return Err(CoreError::InvalidArgument("rule disabled".into()));
        }
        if rule.account_id != *target_account {
            return Err(CoreError::AccountIsolationViolation {
                expected: rule.account_id.clone(),
                actual: target_account.clone(),
            });
        }
        if self.is_circuit_open(&rule.id) {
            return Err(CoreError::Internal(format!(
                "circuit open for automation {}",
                rule.id
            )));
        }
        Ok(AutomationProposal {
            rule_id: rule.id.clone(),
            account_id: rule.account_id.clone(),
            preview: context_preview.to_owned(),
            requires_confirmation: true,
        })
    }

    pub fn record_failure(&self, rule_id: &str) -> CoreResult<()> {
        let mut g = self
            .failure_counts
            .lock()
            .map_err(|_| CoreError::Internal("automation lock".into()))?;
        let c = g.entry(rule_id.to_owned()).or_insert(0);
        *c = c.saturating_add(1);
        if *c >= CIRCUIT_BREAKER_THRESHOLD {
            self.open_circuits
                .lock()
                .map_err(|_| CoreError::Internal("automation lock".into()))?
                .insert(rule_id.to_owned(), true);
        }
        Ok(())
    }

    pub fn is_circuit_open(&self, rule_id: &str) -> bool {
        self.open_circuits
            .lock()
            .ok()
            .and_then(|g| g.get(rule_id).copied())
            .unwrap_or(false)
    }

    pub fn reset_circuit(&self, rule_id: &str) -> CoreResult<()> {
        self.open_circuits
            .lock()
            .map_err(|_| CoreError::Internal("automation lock".into()))?
            .remove(rule_id);
        self.failure_counts
            .lock()
            .map_err(|_| CoreError::Internal("automation lock".into()))?
            .remove(rule_id);
        Ok(())
    }
}

/// Reject attempts to create automation from email body content.
pub fn reject_email_authored_automation(email_body: &str) -> CoreResult<()> {
    let lower = email_body.to_ascii_lowercase();
    if lower.contains("create automation")
        || lower.contains("auto-archive all")
        || lower.contains("approve this rule")
    {
        return Err(CoreError::PermissionDenied);
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn circuit_opens_after_threshold() {
        let eng = AutomationEngine::new();
        for _ in 0..CIRCUIT_BREAKER_THRESHOLD {
            eng.record_failure("r1").unwrap();
        }
        assert!(eng.is_circuit_open("r1"));
    }

    #[test]
    fn email_cannot_author_automation() {
        assert!(reject_email_authored_automation("please create automation now").is_err());
    }

    #[test]
    fn propose_always_needs_confirmation() {
        let eng = AutomationEngine::new();
        let a = AccountId::new("a").unwrap();
        let rule = AutomationRule {
            account_id: a.clone(),
            id: "r".into(),
            name: "Label receipts".into(),
            enabled: true,
            trigger: AutomationTrigger::MessageReceived,
            condition_summary: "subject contains receipt".into(),
            action: AutomationActionKind::ApplyLabel,
            destructive: false,
        };
        let p = eng.propose(&rule, &a, "Would apply label").unwrap();
        assert!(p.requires_confirmation);
    }

    #[test]
    fn propose_rejects_cross_account() {
        let eng = AutomationEngine::new();
        let a = AccountId::new("a").unwrap();
        let b = AccountId::new("b").unwrap();
        let rule = AutomationRule {
            account_id: a,
            id: "r".into(),
            name: "x".into(),
            enabled: true,
            trigger: AutomationTrigger::Manual,
            condition_summary: "".into(),
            action: AutomationActionKind::Notify,
            destructive: false,
        };
        assert!(eng.propose(&rule, &b, "nope").is_err());
    }
}
