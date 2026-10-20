//! Phase 09 — Google OAuth contracts (PKCE S256, system-browser flow).
//!
//! Client secrets / client IDs come from environment or secure config —
//! never hard-coded into source. Tokens live in [`crate::platform::CredentialStore`].

use crate::error::{CoreError, CoreResult};
use crate::platform::SecureSecret;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Mutex;

/// Base scopes: one sign-in grants Gmail + Calendar + Tasks + identity.
pub const SCOPE_GMAIL_READONLY: &str = "https://www.googleapis.com/auth/gmail.readonly";
pub const SCOPE_CALENDAR_EVENTS: &str = "https://www.googleapis.com/auth/calendar.events";
pub const SCOPE_TASKS: &str = "https://www.googleapis.com/auth/tasks";
pub const SCOPE_OPENID: &str = "openid";
pub const SCOPE_EMAIL: &str = "email";
pub const SCOPE_PROFILE: &str = "profile";
/// Incremental, confirmation-gated — not in the default set.
pub const SCOPE_GMAIL_MODIFY: &str = "https://www.googleapis.com/auth/gmail.modify";

pub fn default_scopes() -> Vec<&'static str> {
    vec![
        SCOPE_GMAIL_READONLY,
        SCOPE_CALENDAR_EVENTS,
        SCOPE_TASKS,
        SCOPE_OPENID,
        SCOPE_EMAIL,
        SCOPE_PROFILE,
    ]
}

/// OAuth client configuration loaded from env (no secrets in repo).
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct OAuthClientConfig {
    pub client_id: String,
    pub redirect_uri: String,
}

impl OAuthClientConfig {
    /// Reads `MAILSTACK_OAUTH_CLIENT_ID` and optional `MAILSTACK_OAUTH_REDIRECT`.
    pub fn from_env() -> CoreResult<Self> {
        let client_id = std::env::var("MAILSTACK_OAUTH_CLIENT_ID").map_err(|_| {
            CoreError::PlatformUnavailable(
                "MAILSTACK_OAUTH_CLIENT_ID not set (secrets stay out of source)".into(),
            )
        })?;
        let redirect_uri = std::env::var("MAILSTACK_OAUTH_REDIRECT")
            .unwrap_or_else(|_| "http://127.0.0.1:1/oauth2redirect".into());
        if client_id.trim().is_empty() {
            return Err(CoreError::InvalidArgument("empty client id".into()));
        }
        Ok(Self {
            client_id,
            redirect_uri,
        })
    }

    pub fn for_tests(client_id: &str, redirect: &str) -> Self {
        Self {
            client_id: client_id.into(),
            redirect_uri: redirect.into(),
        }
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct PkcePair {
    pub verifier: String,
    pub challenge: String,
}

/// Generates a PKCE S256 pair (`code_challenge = BASE64URL(SHA256(verifier))`).
pub fn generate_pkce(verifier_bytes: &[u8]) -> PkcePair {
    use sha2::{Digest, Sha256};
    let verifier = base64url_nopad(verifier_bytes);
    let hash = Sha256::digest(verifier.as_bytes());
    let challenge = base64url_nopad(&hash);
    PkcePair {
        verifier,
        challenge,
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct AuthUrlRequest {
    pub client: OAuthClientConfig,
    pub pkce: PkcePair,
    pub state: String,
    pub scopes: Vec<String>,
    pub include_gmail_modify: bool,
}

/// Builds the Google authorization URL with required query params.
pub fn build_auth_url(req: &AuthUrlRequest) -> String {
    let mut scopes = req.scopes.clone();
    if req.include_gmail_modify && !scopes.iter().any(|s| s == SCOPE_GMAIL_MODIFY) {
        scopes.push(SCOPE_GMAIL_MODIFY.into());
    }
    let scope_joined = scopes.join(" ");
    format!(
        "https://accounts.google.com/o/oauth2/v2/auth?client_id={}&redirect_uri={}&response_type=code&scope={}&state={}&code_challenge={}&code_challenge_method=S256&access_type=offline&prompt=consent&include_granted_scopes=true",
        urlenc(&req.client.client_id),
        urlenc(&req.client.redirect_uri),
        urlenc(&scope_joined),
        urlenc(&req.state),
        urlenc(&req.pkce.challenge),
    )
}

/// Pending PKCE flow — consume-once on redirect.
#[derive(Clone, Debug)]
pub struct PendingPkce {
    pub state: String,
    pub verifier: SecureSecret,
    pub consumed: bool,
}

#[derive(Default)]
pub struct PendingPkceStore {
    inner: Mutex<HashMap<String, PendingPkce>>,
}

impl PendingPkceStore {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn put(&self, state: &str, verifier: SecureSecret) -> CoreResult<()> {
        let mut g = self.inner.lock().map_err(|_| CoreError::Internal("lock".into()))?;
        g.insert(
            state.to_owned(),
            PendingPkce {
                state: state.to_owned(),
                verifier,
                consumed: false,
            },
        );
        Ok(())
    }

    /// Replay + consume-once. Second call for the same state fails.
    pub fn consume(&self, state: &str) -> CoreResult<SecureSecret> {
        let mut g = self.inner.lock().map_err(|_| CoreError::Internal("lock".into()))?;
        let entry = g
            .get_mut(state)
            .ok_or_else(|| CoreError::NotFound("pending pkce".into()))?;
        if entry.consumed {
            return Err(CoreError::InvalidArgument(
                "PKCE state already consumed".into(),
            ));
        }
        entry.consumed = true;
        Ok(SecureSecret::from_bytes(entry.verifier.as_bytes().to_vec()))
    }
}

fn urlenc(s: &str) -> String {
    let mut out = String::with_capacity(s.len() * 2);
    for b in s.bytes() {
        match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                out.push(b as char)
            }
            _ => out.push_str(&format!("%{b:02X}")),
        }
    }
    out
}

fn base64url_nopad(data: &[u8]) -> String {
    const T: &[u8] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    let mut out = String::new();
    let mut i = 0;
    while i + 3 <= data.len() {
        let n = ((data[i] as u32) << 16) | ((data[i + 1] as u32) << 8) | (data[i + 2] as u32);
        out.push(T[((n >> 18) & 63) as usize] as char);
        out.push(T[((n >> 12) & 63) as usize] as char);
        out.push(T[((n >> 6) & 63) as usize] as char);
        out.push(T[(n & 63) as usize] as char);
        i += 3;
    }
    let rem = data.len() - i;
    if rem == 1 {
        let n = (data[i] as u32) << 16;
        out.push(T[((n >> 18) & 63) as usize] as char);
        out.push(T[((n >> 12) & 63) as usize] as char);
    } else if rem == 2 {
        let n = ((data[i] as u32) << 16) | ((data[i + 1] as u32) << 8);
        out.push(T[((n >> 18) & 63) as usize] as char);
        out.push(T[((n >> 12) & 63) as usize] as char);
        out.push(T[((n >> 6) & 63) as usize] as char);
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pkce_challenge_is_s256_shaped() {
        let pair = generate_pkce(b"abcdefghijklmnopqrstuvwxyz0123456789abcd");
        assert!(!pair.verifier.is_empty());
        assert!(!pair.challenge.is_empty());
        assert_ne!(pair.verifier, pair.challenge);
    }

    #[test]
    fn auth_url_contains_required_params() {
        let client = OAuthClientConfig::for_tests("cid", "http://127.0.0.1/cb");
        let pkce = generate_pkce(b"0123456789abcdef0123456789abcdef");
        let url = build_auth_url(&AuthUrlRequest {
            client,
            pkce,
            state: "st".into(),
            scopes: default_scopes().into_iter().map(str::to_owned).collect(),
            include_gmail_modify: false,
        });
        assert!(url.contains("code_challenge_method=S256"));
        assert!(url.contains("access_type=offline"));
        assert!(url.contains("prompt=consent"));
        assert!(url.contains("include_granted_scopes=true"));
        assert!(!url.contains("gmail.modify"));
    }

    #[test]
    fn pending_pkce_consume_once() {
        let store = PendingPkceStore::new();
        store
            .put("s1", SecureSecret::from_bytes(b"ver".to_vec()))
            .unwrap();
        let v = store.consume("s1").unwrap();
        assert_eq!(v.expose_utf8().unwrap(), "ver");
        assert!(store.consume("s1").is_err());
    }
}
