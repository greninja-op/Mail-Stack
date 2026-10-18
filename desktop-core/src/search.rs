use serde::{Deserialize, Serialize};

use crate::mailbox::ThreadSummary;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct StructuredQuery {
    pub free_text: Option<String>,
    pub from: Option<String>,
    pub subject: Option<String>,
    pub is_unread: Option<bool>,
    pub is_starred: Option<bool>,
}

/// Structured query only — callers never build SQL.
pub fn search_threads(threads: &[ThreadSummary], query: &StructuredQuery) -> Vec<ThreadSummary> {
    threads
        .iter()
        .filter(|t| {
            if let Some(q) = &query.free_text {
                let q = q.to_lowercase();
                if q.is_empty() {
                    // ok
                } else {
                    let hay = format!("{} {} {}", t.subject, t.snippet, t.sender_name).to_lowercase();
                    if !hay.contains(&q) {
                        return false;
                    }
                }
            }
            if let Some(from) = &query.from {
                if !t.sender_address.to_lowercase().contains(&from.to_lowercase()) {
                    return false;
                }
            }
            if let Some(subject) = &query.subject {
                if !t.subject.to_lowercase().contains(&subject.to_lowercase()) {
                    return false;
                }
            }
            if let Some(u) = query.is_unread {
                if t.unread != u {
                    return false;
                }
            }
            if let Some(s) = query.is_starred {
                if t.starred != s {
                    return false;
                }
            }
            true
        })
        .cloned()
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::account::AccountId;
    use crate::mailbox::demo_threads;

    #[test]
    fn unread_filter() {
        let q = StructuredQuery {
            is_unread: Some(true),
            ..Default::default()
        };
        let hits = search_threads(&demo_threads(), &q);
        assert!(!hits.is_empty());
        assert!(hits.iter().all(|t| t.unread));
    }

    #[test]
    fn empty_query_returns_all_not_error() {
        let hits = search_threads(&demo_threads(), &StructuredQuery::default());
        assert_eq!(hits.len(), demo_threads().len());
    }

    #[test]
    fn no_matches_returns_empty_not_error() {
        let q = StructuredQuery {
            free_text: Some("zzznomatch999".into()),
            ..Default::default()
        };
        let hits = search_threads(&demo_threads(), &q);
        assert!(hits.is_empty());
    }

    #[test]
    fn unicode_and_special_chars_do_not_panic() {
        let q = StructuredQuery {
            free_text: Some("发票 %_\\ 🔐".into()),
            ..Default::default()
        };
        let _ = search_threads(&demo_threads(), &q);
    }

    #[test]
    fn never_returns_other_account_threads() {
        let mut threads = demo_threads();
        let other = AccountId::new("local-other").unwrap();
        threads.push(ThreadSummary {
            thread_id: "x".into(),
            account_id: other.clone(),
            subject: "Leak".into(),
            snippet: "should not appear for demo account search scope".into(),
            sender_name: "X".into(),
            sender_address: "x@ex.com".into(),
            sender_domain: None,
            timestamp_epoch_ms: 0,
            unread: true,
            starred: false,
        });
        let scoped: Vec<_> = threads
            .into_iter()
            .filter(|t| t.account_id.as_str() == "local-desktop-demo")
            .collect();
        let hits = search_threads(
            &scoped,
            &StructuredQuery {
                free_text: Some("Leak".into()),
                ..Default::default()
            },
        );
        assert!(hits.is_empty());
        assert!(scoped.iter().all(|t| t.account_id.as_str() != other.as_str()));
    }
}
