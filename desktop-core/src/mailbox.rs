use serde::{Deserialize, Serialize};

use crate::account::AccountId;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Destination {
    AllInbox,
    Primary,
    Promotional,
    Social,
    Spam,
    Starred,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ThreadSummary {
    pub thread_id: String,
    pub account_id: AccountId,
    pub subject: String,
    pub snippet: String,
    pub sender_name: String,
    pub sender_address: String,
    pub sender_domain: Option<String>,
    pub timestamp_epoch_ms: i64,
    pub unread: bool,
    pub starred: bool,
}

pub fn demo_threads() -> Vec<ThreadSummary> {
    let account = AccountId::new("local-desktop-demo").expect("demo id");
    let now = 1_700_000_000_000i64;
    vec![
        ThreadSummary {
            thread_id: "t1".into(),
            account_id: account.clone(),
            subject: "Welcome to Mailstack Desktop".into(),
            snippet: "Shared Rust core drives macOS, Windows, and Linux.".into(),
            sender_name: "Mailstack".into(),
            sender_address: "hello@mailstack.app".into(),
            sender_domain: Some("mailstack.app".into()),
            timestamp_epoch_ms: now,
            unread: true,
            starred: false,
        },
        ThreadSummary {
            thread_id: "t2".into(),
            account_id: account.clone(),
            subject: "Invoice ready".into(),
            snippet: "Your monthly invoice is ready.".into(),
            sender_name: "Billing".into(),
            sender_address: "billing@acme.example".into(),
            sender_domain: Some("acme.example".into()),
            timestamp_epoch_ms: now - 3_600_000,
            unread: true,
            starred: true,
        },
        ThreadSummary {
            thread_id: "t3".into(),
            account_id: account.clone(),
            subject: "Security alert".into(),
            snippet: "A new sign-in was detected.".into(),
            sender_name: "Security".into(),
            sender_address: "noreply@secure.example".into(),
            sender_domain: Some("secure.example".into()),
            timestamp_epoch_ms: now - 7_200_000,
            unread: true,
            starred: false,
        },
        ThreadSummary {
            thread_id: "t4".into(),
            account_id: account,
            subject: "Weekend sale".into(),
            snippet: "Save 20% this weekend.".into(),
            sender_name: "Shop".into(),
            sender_address: "deals@shop.example".into(),
            sender_domain: Some("shop.example".into()),
            timestamp_epoch_ms: now - 86_400_000,
            unread: false,
            starred: false,
        },
    ]
}

pub fn filter_destination(threads: &[ThreadSummary], destination: Destination) -> Vec<ThreadSummary> {
    threads
        .iter()
        .filter(|t| match destination {
            Destination::AllInbox | Destination::Primary => true,
            Destination::Starred => t.starred,
            Destination::Promotional => t
                .sender_domain
                .as_ref()
                .map(|d| d.contains("shop") || d.contains("store"))
                .unwrap_or(false),
            Destination::Social => t.sender_domain.is_none(),
            Destination::Spam => false,
        })
        .cloned()
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn demo_account_is_local() {
        let t = demo_threads();
        assert!(!t.is_empty());
        assert!(t[0].account_id.is_demo());
    }
}
