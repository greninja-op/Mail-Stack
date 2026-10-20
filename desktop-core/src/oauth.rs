//! OAuth contracts — no client secrets in this crate. Live verification is external.

use crate::account::AccountId;

pub const REDIRECT_URI: &str = "com.greninjaop.mailorganizer:/oauth2redirect";
pub const PKCE_METHOD: &str = "S256";

/// Base scopes — gmail.modify is NEVER here (incremental only).
pub const BASE_SCOPES: &[&str] = &[
    "openid",
    "email",
    "profile",
    "https://www.googleapis.com/auth/gmail.readonly",
    "https://www.googleapis.com/auth/calendar.events",
    "https://www.googleapis.com/auth/tasks",
];

pub fn base_scopes_exclude_modify() -> bool {
    !BASE_SCOPES.iter().any(|s| s.contains("gmail.modify"))
}

/// PKCE S256 challenge = BASE64URL(SHA256(verifier)) — platforms compute; core validates shape.
pub fn looks_like_pkce_verifier(v: &str) -> bool {
    (43..=128).contains(&v.len()) && v.chars().all(|c| c.is_ascii_alphanumeric() || c == '-' || c == '_')
}

#[derive(Debug, Clone)]
pub struct PendingAuth {
    pub account_hint: Option<AccountId>,
    pub state: String,
    pub code_verifier: String,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn redirect_and_pkce_constants() {
        assert_eq!(REDIRECT_URI, "com.greninjaop.mailorganizer:/oauth2redirect");
        assert_eq!(PKCE_METHOD, "S256");
        assert!(base_scopes_exclude_modify());
    }

    #[test]
    fn verifier_shape() {
        assert!(looks_like_pkce_verifier(&"a".repeat(43)));
        assert!(!looks_like_pkce_verifier("short"));
    }
}
