//! Message / thread presentation models.
//!
//! Row models **must** carry raw sender email + display name for the logo pipeline.

use crate::account::{AccountId, AccountScoped};
use crate::category::CategoryGroupKind;
use crate::logo::LogoPipelineInput;
use serde::{Deserialize, Serialize};

/// Stable ids (opaque strings from Gmail).
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct MessageId(pub String);

#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct ThreadId(pub String);

/// Attachment presentation stub (no bytes — metadata only).
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct AttachmentMeta {
    pub filename: String,
    pub mime_type: String,
    pub size_bytes: u64,
}

/// Presentation row for mail lists and category detail.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct MessagePresentation {
    pub account_id: AccountId,
    pub message_id: MessageId,
    pub thread_id: ThreadId,
    /// Raw sender email — required for logo / company domain detection.
    pub sender_email: String,
    pub sender_display_name: String,
    pub subject: String,
    pub snippet: String,
    pub internal_date_ms: i64,
    pub is_unread: bool,
    /// Message star (distinct from company pin).
    pub is_starred: bool,
    pub label_ids: Vec<String>,
    pub category: Option<CategoryGroupKind>,
    pub attachments: Vec<AttachmentMeta>,
    /// Receiving account email for multi-account indicator (not sender).
    pub receiving_account_email: String,
}

impl AccountScoped for MessagePresentation {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

impl MessagePresentation {
    /// Logo pipeline input — never drop sender email from the row model.
    pub fn logo_input(&self) -> LogoPipelineInput {
        LogoPipelineInput::new(&self.sender_email, &self.sender_display_name)
    }

    /// Defensive parse: empty/malformed sender still yields a usable row.
    pub fn from_parts(
        account_id: AccountId,
        receiving_account_email: impl Into<String>,
        message_id: impl Into<String>,
        thread_id: impl Into<String>,
        sender_email: impl Into<String>,
        sender_display_name: impl Into<String>,
        subject: impl Into<String>,
        snippet: impl Into<String>,
        internal_date_ms: i64,
    ) -> Self {
        let sender_email = sender_email.into();
        let sender_display_name = sender_display_name.into();
        Self {
            account_id,
            message_id: MessageId(message_id.into()),
            thread_id: ThreadId(thread_id.into()),
            sender_email,
            sender_display_name,
            subject: subject.into(),
            snippet: snippet.into(),
            internal_date_ms,
            is_unread: false,
            is_starred: false,
            label_ids: Vec::new(),
            category: None,
            attachments: Vec::new(),
            receiving_account_email: receiving_account_email.into(),
        }
    }
}

/// Thread presentation: ordered messages + aggregate flags.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct ThreadPresentation {
    pub account_id: AccountId,
    pub thread_id: ThreadId,
    pub messages: Vec<MessagePresentation>,
    pub subject: String,
    pub has_unread: bool,
    pub has_starred: bool,
}

impl AccountScoped for ThreadPresentation {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

impl ThreadPresentation {
    pub fn from_messages(account_id: AccountId, thread_id: ThreadId, messages: Vec<MessagePresentation>) -> Self {
        let subject = messages
            .first()
            .map(|m| m.subject.clone())
            .unwrap_or_default();
        let has_unread = messages.iter().any(|m| m.is_unread);
        let has_starred = messages.iter().any(|m| m.is_starred);
        Self {
            account_id,
            thread_id,
            messages,
            subject,
            has_unread,
            has_starred,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn row_preserves_sender_email_for_logo() {
        let a = AccountId::new("a").unwrap();
        let m = MessagePresentation::from_parts(
            a,
            "me@ex.com",
            "m1",
            "t1",
            "noreply@mail.stripe.com",
            "Stripe",
            "Receipt",
            "Thanks",
            1,
        );
        assert_eq!(
            m.logo_input().detect_sender_domain().unwrap().as_str(),
            "stripe.com"
        );
    }

    #[test]
    fn thread_aggregates_flags() {
        let a = AccountId::new("a").unwrap();
        let mut m1 = MessagePresentation::from_parts(
            a.clone(),
            "me@ex.com",
            "m1",
            "t1",
            "a@b.com",
            "A",
            "Hi",
            "",
            1,
        );
        m1.is_unread = true;
        let mut m2 = MessagePresentation::from_parts(
            a.clone(),
            "me@ex.com",
            "m2",
            "t1",
            "a@b.com",
            "A",
            "Hi",
            "",
            2,
        );
        m2.is_starred = true;
        let t = ThreadPresentation::from_messages(a, ThreadId("t1".into()), vec![m1, m2]);
        assert!(t.has_unread);
        assert!(t.has_starred);
    }
}
