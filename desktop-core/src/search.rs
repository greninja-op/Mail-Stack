//! Structured search query DSL + FTS builder (escaped) + 300ms debounce helper.
//!
//! UI never builds SQL. Every FTS term is escaped (injection-proof by construction).

use crate::account::AccountId;
use crate::category::CategoryGroupKind;
use serde::{Deserialize, Serialize};
use std::time::{Duration, Instant};

/// Debounce interval for search-as-you-type.
pub const SEARCH_DEBOUNCE: Duration = Duration::from_millis(300);

/// Structured query — the only search input accepted by the engine.
#[derive(Clone, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct SearchQuery {
    pub text: Option<String>,
    pub sender: Option<String>,
    pub subject: Option<String>,
    pub company_domain: Option<String>,
    pub category: Option<CategoryGroupKind>,
    pub priority_min: Option<u8>,
    pub action_required: Option<bool>,
    pub label: Option<String>,
    /// Empty = all enabled accounts (unified join).
    pub account_ids: Vec<AccountId>,
}

/// Escapes a single FTS5 term: wraps in double quotes and doubles internal quotes.
pub fn escape_fts_term(term: &str) -> String {
    let trimmed = term.trim();
    if trimmed.is_empty() {
        return String::new();
    }
    let escaped = trimmed.replace('"', "\"\"");
    format!("\"{escaped}\"")
}

/// Builds an FTS MATCH expression from a structured query. Never interpolates
/// raw user text into SQL outside quoted escaped terms.
pub fn build_fts_match(query: &SearchQuery) -> Option<String> {
    let mut parts = Vec::new();
    if let Some(t) = query.text.as_deref() {
        for token in t.split_whitespace() {
            let e = escape_fts_term(token);
            if !e.is_empty() {
                parts.push(e);
            }
        }
    }
    if let Some(s) = query.sender.as_deref() {
        let e = escape_fts_term(s);
        if !e.is_empty() {
            parts.push(format!("sender_email:{e}"));
        }
    }
    if let Some(s) = query.subject.as_deref() {
        let e = escape_fts_term(s);
        if !e.is_empty() {
            parts.push(format!("subject:{e}"));
        }
    }
    if let Some(d) = query.company_domain.as_deref() {
        let e = escape_fts_term(d);
        if !e.is_empty() {
            parts.push(e);
        }
    }
    if parts.is_empty() {
        None
    } else {
        Some(parts.join(" "))
    }
}

/// Simple debounce gate: returns true when `now` is ≥ last_fire + SEARCH_DEBOUNCE
/// since `last_input` (caller records last_input on each keystroke).
#[derive(Clone, Debug)]
pub struct SearchDebouncer {
    last_input: Option<Instant>,
    last_fire: Option<Instant>,
}

impl Default for SearchDebouncer {
    fn default() -> Self {
        Self::new()
    }
}

impl SearchDebouncer {
    pub fn new() -> Self {
        Self {
            last_input: None,
            last_fire: None,
        }
    }

    pub fn on_input(&mut self, now: Instant) {
        self.last_input = Some(now);
    }

    /// Whether a search should run at `now` (300ms quiet after last input).
    pub fn should_fire(&mut self, now: Instant) -> bool {
        let Some(last_input) = self.last_input else {
            return false;
        };
        if now.duration_since(last_input) < SEARCH_DEBOUNCE {
            return false;
        }
        if let Some(last_fire) = self.last_fire {
            if last_fire >= last_input {
                return false;
            }
        }
        self.last_fire = Some(now);
        true
    }

    pub const fn debounce_ms() -> u64 {
        300
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn escape_prevents_injection() {
        let evil = r#"foo" OR 1=1 --"#;
        let e = escape_fts_term(evil);
        assert_eq!(e, "\"foo\"\" OR 1=1 --\"");
        assert!(!e.contains(" OR 1=1") || e.starts_with('"'));
    }

    #[test]
    fn build_fts_from_structured_query() {
        let q = SearchQuery {
            text: Some("hello world".into()),
            sender: Some("a@b.com".into()),
            ..Default::default()
        };
        let m = build_fts_match(&q).unwrap();
        assert!(m.contains("\"hello\""));
        assert!(m.contains("\"world\""));
        assert!(m.contains("sender_email:\"a@b.com\""));
    }

    #[test]
    fn debounce_is_300ms() {
        assert_eq!(SearchDebouncer::debounce_ms(), 300);
        let mut d = SearchDebouncer::new();
        let t0 = Instant::now();
        d.on_input(t0);
        assert!(!d.should_fire(t0 + Duration::from_millis(100)));
        assert!(d.should_fire(t0 + Duration::from_millis(300)));
        // Already fired for this input.
        assert!(!d.should_fire(t0 + Duration::from_millis(400)));
    }
}
