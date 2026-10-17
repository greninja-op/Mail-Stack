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
}
