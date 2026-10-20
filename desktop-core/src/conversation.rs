//! Conversation intelligence — 7 deterministic states.
//!
//! Never equate "no reply" with "user forgot". Negative signals shield false awaiting-reply.

use crate::account::{AccountId, AccountScoped};
use serde::{Deserialize, Serialize};

/// Follow-up candidate window (days).
pub const FOLLOW_UP_CANDIDATE_DAYS: i64 = 3;
/// Stale conversation threshold (days).
pub const STALE_THRESHOLD_DAYS: i64 = 7;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ConversationState {
    NoAction,
    AwaitingUserReply,
    AwaitingOtherParty,
    RecentlyReplied,
    StaleConversation,
    Resolved,
    Unknown,
}

impl ConversationState {
    pub const ALL: [ConversationState; 7] = [
        Self::NoAction,
        Self::AwaitingUserReply,
        Self::AwaitingOtherParty,
        Self::RecentlyReplied,
        Self::StaleConversation,
        Self::Resolved,
        Self::Unknown,
    ];

    pub fn display_label(self) -> &'static str {
        match self {
            Self::NoAction => "No action needed",
            Self::AwaitingUserReply => "Awaiting your reply",
            Self::AwaitingOtherParty => "Waiting for response",
            Self::RecentlyReplied => "Recently replied",
            Self::StaleConversation => "Stale conversation",
            Self::Resolved => "Resolved",
            Self::Unknown => "Unknown",
        }
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct ConversationAssessment {
    pub account_id: AccountId,
    pub thread_id: String,
    pub state: ConversationState,
    pub explanation: String,
    pub reasons: Vec<String>,
}

impl AccountScoped for ConversationAssessment {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

#[derive(Clone, Debug, Default)]
pub struct ConversationSignals {
    pub last_message_from_user: bool,
    pub last_message_expects_reply: bool,
    pub days_since_last_activity: i64,
    pub user_replied_recently: bool,
    pub resolved_language: bool,
    /// Newsletters, notifications, receipts, spam, no-reply — shield awaiting-reply.
    pub negative_signal: bool,
    pub has_sufficient_evidence: bool,
}

/// Deterministic state machine.
pub fn assess_conversation(
    account_id: AccountId,
    thread_id: impl Into<String>,
    signals: &ConversationSignals,
) -> ConversationAssessment {
    let mut reasons = Vec::new();

    if signals.negative_signal {
        reasons.push("Negative signal (newsletter/notification/receipt/spam/no-reply)".into());
        return ConversationAssessment {
            account_id,
            thread_id: thread_id.into(),
            state: ConversationState::NoAction,
            explanation: "Passive or automated thread — not awaiting a personal reply".into(),
            reasons,
        };
    }

    if signals.resolved_language {
        reasons.push("Resolution / closure language".into());
        return ConversationAssessment {
            account_id,
            thread_id: thread_id.into(),
            state: ConversationState::Resolved,
            explanation: "Thread appears resolved".into(),
            reasons,
        };
    }

    if !signals.has_sufficient_evidence {
        reasons.push("Insufficient or conflicting evidence".into());
        return ConversationAssessment {
            account_id,
            thread_id: thread_id.into(),
            state: ConversationState::Unknown,
            explanation: "Not enough signal to infer conversation state".into(),
            reasons,
        };
    }

    if signals.user_replied_recently && signals.last_message_from_user {
        reasons.push("User replied within the recent window".into());
        return ConversationAssessment {
            account_id,
            thread_id: thread_id.into(),
            state: ConversationState::RecentlyReplied,
            explanation: ConversationState::RecentlyReplied.display_label().into(),
            reasons,
        };
    }

    if signals.last_message_from_user && signals.last_message_expects_reply {
        reasons.push("Latest message from user appears to await a response".into());
        if signals.days_since_last_activity >= STALE_THRESHOLD_DAYS {
            reasons.push(format!(
                "No activity for ≥{STALE_THRESHOLD_DAYS} days"
            ));
            return ConversationAssessment {
                account_id,
                thread_id: thread_id.into(),
                state: ConversationState::StaleConversation,
                explanation: "Waiting on other party, but conversation looks stale".into(),
                reasons,
            };
        }
        return ConversationAssessment {
            account_id,
            thread_id: thread_id.into(),
            state: ConversationState::AwaitingOtherParty,
            explanation: ConversationState::AwaitingOtherParty.display_label().into(),
            reasons,
        };
    }

    if !signals.last_message_from_user && signals.last_message_expects_reply {
        reasons.push("Latest message from other party appears to expect a reply".into());
        if signals.days_since_last_activity >= STALE_THRESHOLD_DAYS {
            reasons.push(format!(
                "No activity for ≥{STALE_THRESHOLD_DAYS} days"
            ));
            return ConversationAssessment {
                account_id,
                thread_id: thread_id.into(),
                state: ConversationState::StaleConversation,
                explanation: "Awaiting your reply, but conversation looks stale".into(),
                reasons,
            };
        }
        return ConversationAssessment {
            account_id,
            thread_id: thread_id.into(),
            state: ConversationState::AwaitingUserReply,
            explanation: ConversationState::AwaitingUserReply.display_label().into(),
            reasons,
        };
    }

    reasons.push("No pending response signals".into());
    ConversationAssessment {
        account_id,
        thread_id: thread_id.into(),
        state: ConversationState::NoAction,
        explanation: ConversationState::NoAction.display_label().into(),
        reasons,
    }
}

/// Whether a thread is a follow-up candidate (3-day window).
pub fn is_follow_up_candidate(signals: &ConversationSignals) -> bool {
    signals.last_message_from_user
        && signals.last_message_expects_reply
        && !signals.negative_signal
        && signals.days_since_last_activity >= FOLLOW_UP_CANDIDATE_DAYS
        && signals.days_since_last_activity < STALE_THRESHOLD_DAYS
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn seven_states() {
        assert_eq!(ConversationState::ALL.len(), 7);
    }

    #[test]
    fn negative_signal_shields_awaiting_reply() {
        let a = AccountId::new("a").unwrap();
        let r = assess_conversation(
            a,
            "t1",
            &ConversationSignals {
                last_message_from_user: false,
                last_message_expects_reply: true,
                negative_signal: true,
                has_sufficient_evidence: true,
                ..Default::default()
            },
        );
        assert_eq!(r.state, ConversationState::NoAction);
    }

    #[test]
    fn stale_after_seven_days() {
        let a = AccountId::new("a").unwrap();
        let r = assess_conversation(
            a,
            "t1",
            &ConversationSignals {
                last_message_from_user: false,
                last_message_expects_reply: true,
                days_since_last_activity: 8,
                has_sufficient_evidence: true,
                ..Default::default()
            },
        );
        assert_eq!(r.state, ConversationState::StaleConversation);
    }

    #[test]
    fn never_implies_user_forgot() {
        // Silence / no reply alone → Unknown or NoAction, never a "forgot" label.
        let a = AccountId::new("a").unwrap();
        let r = assess_conversation(
            a,
            "t",
            &ConversationSignals {
                has_sufficient_evidence: false,
                ..Default::default()
            },
        );
        assert_eq!(r.state, ConversationState::Unknown);
        assert!(!r.explanation.to_ascii_lowercase().contains("forgot"));
    }
}
