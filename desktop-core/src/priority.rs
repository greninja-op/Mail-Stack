//! Priority + Action Required with explanations.
//!
//! No mail-list Action Required chip/filter flag — AR is a destination, not a list chip.

use crate::account::{AccountId, AccountScoped};
use crate::rules::DecisionSource;
use serde::{Deserialize, Serialize};

/// Priority levels (higher = more urgent).
#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize)]
pub enum PriorityLevel {
    None = 0,
    Low = 1,
    Medium = 2,
    High = 3,
    Urgent = 4,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct PriorityAssessment {
    pub account_id: AccountId,
    pub message_id: String,
    pub level: PriorityLevel,
    pub source: DecisionSource,
    /// "Why this priority?"
    pub explanation: String,
}

impl AccountScoped for PriorityAssessment {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct ActionRequiredAssessment {
    pub account_id: AccountId,
    pub message_id: String,
    pub is_action_required: bool,
    pub source: DecisionSource,
    pub explanation: String,
}

impl AccountScoped for ActionRequiredAssessment {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

/// UI presentation flags for the Mail list. Intentionally has **no**
/// `show_action_required_chip` — AR chip was removed from the mail list.
#[derive(Clone, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct MailListBadgeFlags {
    pub show_priority_badge: bool,
    pub show_category_accent: bool,
    // deliberately no show_action_required_chip
}

impl MailListBadgeFlags {
    pub fn for_priority(level: PriorityLevel) -> Self {
        Self {
            show_priority_badge: level >= PriorityLevel::Medium,
            show_category_accent: true,
        }
    }

    /// Compile-time reminder: mail list must not expose an AR chip flag.
    pub const fn supports_action_required_chip() -> bool {
        false
    }
}

#[derive(Clone, Debug, Default)]
pub struct PrioritySignals {
    pub subject: String,
    pub snippet: String,
    pub has_deadline_language: bool,
    pub from_known_vip: bool,
}

pub fn assess_priority(
    account_id: AccountId,
    message_id: impl Into<String>,
    signals: &PrioritySignals,
) -> PriorityAssessment {
    let text = format!(
        "{} {}",
        signals.subject.to_ascii_lowercase(),
        signals.snippet.to_ascii_lowercase()
    );
    let (level, explanation) = if signals.has_deadline_language
        || text.contains("urgent")
        || text.contains("asap")
    {
        (
            PriorityLevel::Urgent,
            "Deadline / urgency language detected".into(),
        )
    } else if signals.from_known_vip || text.contains("action required") {
        (
            PriorityLevel::High,
            "VIP sender or explicit action-required phrasing".into(),
        )
    } else if text.contains("please review") || text.contains("reminder") {
        (
            PriorityLevel::Medium,
            "Review / reminder cues".into(),
        )
    } else if text.contains("fyi") || text.contains("newsletter") {
        (PriorityLevel::Low, "Informational cues".into())
    } else {
        (PriorityLevel::None, "No priority signals".into())
    };

    PriorityAssessment {
        account_id,
        message_id: message_id.into(),
        level,
        source: DecisionSource::Deterministic,
        explanation,
    }
}

pub fn assess_action_required(
    account_id: AccountId,
    message_id: impl Into<String>,
    signals: &PrioritySignals,
) -> ActionRequiredAssessment {
    let text = format!(
        "{} {}",
        signals.subject.to_ascii_lowercase(),
        signals.snippet.to_ascii_lowercase()
    );
    let is_ar = signals.has_deadline_language
        || text.contains("action required")
        || text.contains("please respond")
        || text.contains("rsvp");
    ActionRequiredAssessment {
        account_id,
        message_id: message_id.into(),
        is_action_required: is_ar,
        source: DecisionSource::Deterministic,
        explanation: if is_ar {
            "Response or deadline signals present".into()
        } else {
            "No action-required signals".into()
        },
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn no_mail_list_ar_chip_flag() {
        assert!(!MailListBadgeFlags::supports_action_required_chip());
        let f = MailListBadgeFlags::for_priority(PriorityLevel::High);
        assert!(f.show_priority_badge);
    }

    #[test]
    fn urgent_from_deadline() {
        let a = AccountId::new("a").unwrap();
        let p = assess_priority(
            a,
            "m1",
            &PrioritySignals {
                subject: "ASAP please".into(),
                ..Default::default()
            },
        );
        assert_eq!(p.level, PriorityLevel::Urgent);
        assert!(!p.explanation.is_empty());
    }
}
