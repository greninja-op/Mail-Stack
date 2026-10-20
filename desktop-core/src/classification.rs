//! Category classification results with explainability.
//!
//! Company grouping/filtering lives inside the selected category (see [`crate::company`]).

use crate::account::{AccountId, AccountScoped};
use crate::category::CategoryGroupKind;
use crate::rules::DecisionSource;
use serde::{Deserialize, Serialize};

/// Explainable category assignment for one message.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct ClassificationResult {
    pub account_id: AccountId,
    pub message_id: String,
    pub category: CategoryGroupKind,
    pub source: DecisionSource,
    /// "Why this category?" — UI presents, never reimplements.
    pub explanation: String,
    /// 0–100; AI capped separately in [`crate::ai`].
    pub confidence: u8,
}

impl AccountScoped for ClassificationResult {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

/// Lightweight signals used by deterministic classifiers (no network).
#[derive(Clone, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct ClassificationSignals {
    pub subject: String,
    pub snippet: String,
    pub sender_email: String,
    pub label_ids: Vec<String>,
}

/// Deterministic category hints (base layer under rules/corrections).
pub fn classify_deterministic(signals: &ClassificationSignals) -> ClassificationResult {
    let subj = signals.subject.to_ascii_lowercase();
    let snip = signals.snippet.to_ascii_lowercase();
    let combined = format!("{subj} {snip}");

    let (category, explanation) = if signals.label_ids.iter().any(|l| l == "SPAM") {
        (
            CategoryGroupKind::EverythingElse,
            "Gmail Spam label".into(),
        )
    } else if combined.contains("unsubscribe")
        || combined.contains("% off")
        || combined.contains("sale")
        || combined.contains("deal")
    {
        (
            CategoryGroupKind::Promotions,
            "Promotional language in subject/snippet".into(),
        )
    } else if combined.contains("invoice")
        || combined.contains("receipt")
        || combined.contains("statement")
        || combined.contains("shipped")
    {
        (
            CategoryGroupKind::Updates,
            "Transactional update cues".into(),
        )
    } else if combined.contains("assignment")
        || combined.contains("deadline")
        || combined.contains("meeting")
        || combined.contains("interview")
    {
        (
            CategoryGroupKind::NeedsAttention,
            "Action / deadline language".into(),
        )
    } else if combined.contains("course")
        || combined.contains("lecture")
        || combined.contains("learning")
        || signals.sender_email.contains(".edu")
    {
        (
            CategoryGroupKind::WorkAndLearning,
            "Work & learning cues".into(),
        )
    } else {
        (
            CategoryGroupKind::EverythingElse,
            "No strong category signals".into(),
        )
    };

    ClassificationResult {
        account_id: AccountId::new("pending").unwrap(),
        message_id: String::new(),
        category,
        source: DecisionSource::Deterministic,
        explanation,
        confidence: 70,
    }
}

/// Binds a deterministic result to a concrete account/message.
pub fn bind_result(
    account_id: AccountId,
    message_id: impl Into<String>,
    mut result: ClassificationResult,
) -> ClassificationResult {
    result.account_id = account_id;
    result.message_id = message_id.into();
    result
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn promotions_detected() {
        let r = classify_deterministic(&ClassificationSignals {
            subject: "Flash sale — 50% off".into(),
            ..Default::default()
        });
        assert_eq!(r.category, CategoryGroupKind::Promotions);
        assert!(!r.explanation.is_empty());
    }

    #[test]
    fn needs_attention_for_deadline() {
        let r = classify_deterministic(&ClassificationSignals {
            subject: "Project deadline Friday".into(),
            ..Default::default()
        });
        assert_eq!(r.category, CategoryGroupKind::NeedsAttention);
    }
}
