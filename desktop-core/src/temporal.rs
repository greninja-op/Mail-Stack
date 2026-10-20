//! Temporal intelligence — 14 item types.
//!
//! Reference time = message received. Ambiguous input degrades honestly.

use crate::account::{AccountId, AccountScoped};
use serde::{Deserialize, Serialize};

/// Controlled temporal entity types (14).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum TemporalItemType {
    Deadline,
    Meeting,
    Appointment,
    Event,
    Interview,
    SubmissionDeadline,
    ApplicationDeadline,
    PaymentDeadline,
    RegistrationDeadline,
    ReminderDate,
    DateOnly,
    TimeOnly,
    DateTime,
    DateRange,
}

impl TemporalItemType {
    pub const ALL: [TemporalItemType; 14] = [
        Self::Deadline,
        Self::Meeting,
        Self::Appointment,
        Self::Event,
        Self::Interview,
        Self::SubmissionDeadline,
        Self::ApplicationDeadline,
        Self::PaymentDeadline,
        Self::RegistrationDeadline,
        Self::ReminderDate,
        Self::DateOnly,
        Self::TimeOnly,
        Self::DateTime,
        Self::DateRange,
    ];

    pub fn as_str(self) -> &'static str {
        match self {
            Self::Deadline => "DEADLINE",
            Self::Meeting => "MEETING",
            Self::Appointment => "APPOINTMENT",
            Self::Event => "EVENT",
            Self::Interview => "INTERVIEW",
            Self::SubmissionDeadline => "SUBMISSION_DEADLINE",
            Self::ApplicationDeadline => "APPLICATION_DEADLINE",
            Self::PaymentDeadline => "PAYMENT_DEADLINE",
            Self::RegistrationDeadline => "REGISTRATION_DEADLINE",
            Self::ReminderDate => "REMINDER_DATE",
            Self::DateOnly => "DATE_ONLY",
            Self::TimeOnly => "TIME_ONLY",
            Self::DateTime => "DATE_TIME",
            Self::DateRange => "DATE_RANGE",
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum TemporalConfidence {
    Low,
    Medium,
    High,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct TemporalItem {
    pub account_id: AccountId,
    pub message_id: String,
    pub item_type: TemporalItemType,
    /// Start instant as epoch ms when known; None if ambiguous.
    pub start_ms: Option<i64>,
    pub end_ms: Option<i64>,
    pub confidence: TemporalConfidence,
    pub explanation: String,
    /// Idempotency key: messageId|type|start|matched_span
    pub dedup_key: String,
}

impl AccountScoped for TemporalItem {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

#[derive(Clone, Debug, Default)]
pub struct TemporalExtractionInput {
    pub subject: String,
    pub snippet: String,
    pub body_text: String,
    /// Reference time = message received (never wall-clock "now").
    pub received_at_ms: i64,
}

/// Deterministic extractor stub: pattern cues → typed items. Idempotent.
pub fn extract_temporal(
    account_id: AccountId,
    message_id: &str,
    input: &TemporalExtractionInput,
) -> Vec<TemporalItem> {
    let text = format!(
        "{} {} {}",
        input.subject.to_ascii_lowercase(),
        input.snippet.to_ascii_lowercase(),
        input.body_text.to_ascii_lowercase()
    );
    let mut out = Vec::new();

    let push = |out: &mut Vec<TemporalItem>,
                item_type: TemporalItemType,
                explanation: &str,
                confidence: TemporalConfidence| {
        let dedup_key = format!(
            "{}|{}|{}",
            message_id,
            item_type.as_str(),
            explanation
        );
        if out.iter().any(|i| i.dedup_key == dedup_key) {
            return;
        }
        out.push(TemporalItem {
            account_id: account_id.clone(),
            message_id: message_id.to_owned(),
            item_type,
            start_ms: Some(input.received_at_ms),
            end_ms: None,
            confidence,
            explanation: explanation.into(),
            dedup_key,
        });
    };

    if text.contains("deadline") {
        push(
            &mut out,
            TemporalItemType::Deadline,
            "deadline language",
            TemporalConfidence::Medium,
        );
    }
    if text.contains("interview") {
        push(
            &mut out,
            TemporalItemType::Interview,
            "interview language",
            TemporalConfidence::High,
        );
    }
    if text.contains("meeting") || text.contains("sync with") {
        push(
            &mut out,
            TemporalItemType::Meeting,
            "meeting language",
            TemporalConfidence::Medium,
        );
    }
    if text.contains("appointment") {
        push(
            &mut out,
            TemporalItemType::Appointment,
            "appointment language",
            TemporalConfidence::Medium,
        );
    }
    if text.contains("payment due") || text.contains("invoice due") {
        push(
            &mut out,
            TemporalItemType::PaymentDeadline,
            "payment deadline language",
            TemporalConfidence::Medium,
        );
    }
    if text.contains("submit by") || text.contains("submission") {
        push(
            &mut out,
            TemporalItemType::SubmissionDeadline,
            "submission language",
            TemporalConfidence::Medium,
        );
    }
    if text.contains("application due") || text.contains("apply by") {
        push(
            &mut out,
            TemporalItemType::ApplicationDeadline,
            "application deadline language",
            TemporalConfidence::Medium,
        );
    }
    if text.contains("register by") || text.contains("registration") {
        push(
            &mut out,
            TemporalItemType::RegistrationDeadline,
            "registration language",
            TemporalConfidence::Low,
        );
    }
    if text.contains("reminder") {
        push(
            &mut out,
            TemporalItemType::ReminderDate,
            "reminder language",
            TemporalConfidence::Low,
        );
    }
    if text.contains("event") && !text.contains("meeting") {
        push(
            &mut out,
            TemporalItemType::Event,
            "event language",
            TemporalConfidence::Low,
        );
    }

    // Ambiguous bare date/time cues — low confidence; distinct from typed deadlines.
    let has_slash_date = text.contains('/')
        && text.chars().any(|c| c.is_ascii_digit());
    let has_clock = text.contains("am") || text.contains("pm") || text.contains(':');
    if has_slash_date && has_clock {
        push(
            &mut out,
            TemporalItemType::DateTime,
            "combined date+time cue (ambiguous)",
            TemporalConfidence::Low,
        );
    } else if has_slash_date {
        push(
            &mut out,
            TemporalItemType::DateOnly,
            "date-only cue (ambiguous)",
            TemporalConfidence::Low,
        );
    } else if has_clock && (text.contains("at ") || text.contains("starts at")) {
        push(
            &mut out,
            TemporalItemType::TimeOnly,
            "time-only cue (ambiguous)",
            TemporalConfidence::Low,
        );
    }
    if text.contains(" through") || text.contains(" until ") || text.contains("–") {
        push(
            &mut out,
            TemporalItemType::DateRange,
            "range language (ambiguous)",
            TemporalConfidence::Low,
        );
    }

    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fourteen_types() {
        assert_eq!(TemporalItemType::ALL.len(), 14);
    }

    #[test]
    fn extract_is_idempotent() {
        let a = AccountId::new("a").unwrap();
        let input = TemporalExtractionInput {
            subject: "Project deadline Friday".into(),
            snippet: String::new(),
            body_text: String::new(),
            received_at_ms: 1_000,
        };
        let once = extract_temporal(a.clone(), "m1", &input);
        let twice = extract_temporal(a, "m1", &input);
        assert_eq!(once, twice);
        assert!(once.iter().any(|i| i.item_type == TemporalItemType::Deadline));
    }
}
