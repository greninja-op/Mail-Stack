//! macOS UI has not been touched. This is backend only. The UI still needs to be built; the previous UI was really bad and was removed.

#![forbid(unsafe_code)]

use desktop_core::account::AccountId;
use desktop_core::error::{CoreError, CoreResult};
use desktop_core::oauth::{
    build_auth_url, default_scopes, generate_pkce, AuthUrlRequest, OAuthClientConfig, PkcePair,
};
use desktop_core::pacing::CursorState;
use desktop_core::persistence::{AccountRecord, LocalStore, MemoryStore, StoredMessage};
use desktop_core::platform::BrowserOpen;

/// In-memory store wrapper around [`MemoryStore`].
///
/// There is no SQLite file yet. Tokens must not be stored in ordinary rows;
/// credentials belong in the platform credential store, never in account or
/// message records.
pub struct InMemoryStore {
    inner: MemoryStore,
}

impl InMemoryStore {
    pub fn new() -> Self {
        Self {
            inner: MemoryStore::new(),
        }
    }

    pub fn inner(&self) -> &MemoryStore {
        &self.inner
    }
}

impl Default for InMemoryStore {
    fn default() -> Self {
        Self::new()
    }
}

impl LocalStore for InMemoryStore {
    fn upsert_account(&self, account: &AccountRecord) -> CoreResult<()> {
        self.inner.upsert_account(account)
    }

    fn get_account(&self, account_id: &AccountId) -> CoreResult<Option<AccountRecord>> {
        self.inner.get_account(account_id)
    }

    fn list_accounts(&self) -> CoreResult<Vec<AccountRecord>> {
        self.inner.list_accounts()
    }

    fn persist_page_then_cursor(
        &self,
        account_id: &AccountId,
        messages: &[StoredMessage],
        cursor: &CursorState,
    ) -> CoreResult<()> {
        self.inner
            .persist_page_then_cursor(account_id, messages, cursor)
    }

    fn load_cursor(&self, account_id: &AccountId) -> CoreResult<CursorState> {
        self.inner.load_cursor(account_id)
    }

    fn recent_inserted(
        &self,
        account_id: &AccountId,
        limit: usize,
    ) -> CoreResult<Vec<StoredMessage>> {
        self.inner.recent_inserted(account_id, limit)
    }

    fn applied_migration_version(&self) -> CoreResult<u32> {
        self.inner.applied_migration_version()
    }
}

/// OAuth host. Builds the real PKCE authorization URL through `desktop_core::oauth`.
///
/// This host does not invent an auth code, token, or connected account.
/// Opening a browser is refused: no macOS browser launcher exists on this Windows machine.
pub struct OAuthHost {
    client: OAuthClientConfig,
}

/// PKCE material and the Google authorization URL. Not a token and not a connected account.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct AuthorizationStart {
    pub pkce: PkcePair,
    pub url: String,
}

impl OAuthHost {
    pub fn new(client: OAuthClientConfig) -> Self {
        Self { client }
    }

    /// Build a real S256 PKCE pair and the Google auth URL. Does not complete sign-in.
    pub fn start_authorization(&self, verifier_bytes: &[u8], state: &str) -> AuthorizationStart {
        let pkce = generate_pkce(verifier_bytes);
        let url = build_auth_url(&AuthUrlRequest {
            client: self.client.clone(),
            pkce: pkce.clone(),
            state: state.to_owned(),
            scopes: default_scopes().into_iter().map(str::to_owned).collect(),
            include_gmail_modify: false,
        });
        AuthorizationStart { pkce, url }
    }

    /// Browser open for the system OAuth window.
    ///
    /// Always returns [`CoreError::PlatformUnavailable`]: no macOS browser launcher
    /// exists on this Windows machine.
    pub fn open_browser(&self, url: &str) -> CoreResult<()> {
        let _ = url;
        Err(CoreError::PlatformUnavailable(
            "no macOS browser launcher exists on this Windows machine".into(),
        ))
    }
}

impl BrowserOpen for OAuthHost {
    fn open_url(&self, url: &str) -> CoreResult<()> {
        self.open_browser(url)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn host() -> OAuthHost {
        OAuthHost::new(OAuthClientConfig::for_tests(
            "test-client",
            "http://127.0.0.1/oauth2redirect",
        ))
    }

    #[test]
    fn pkce_challenge_differs_from_verifier() {
        let started = host().start_authorization(b"abcdefghijklmnopqrstuvwxyz0123456789abcd", "state-1");
        assert_ne!(started.pkce.verifier, started.pkce.challenge);
        assert!(!started.pkce.verifier.is_empty());
        assert!(!started.pkce.challenge.is_empty());
    }

    #[test]
    fn auth_url_has_readonly_gmail_scope_and_no_client_secret() {
        let started = host().start_authorization(b"0123456789abcdef0123456789abcdef", "state-1");
        assert!(
            started.url.contains("gmail.readonly"),
            "auth URL missing readonly gmail scope: {}",
            started.url
        );
        assert!(
            !started.url.contains("client_secret"),
            "auth URL must not carry a client secret"
        );
        assert!(!started.url.contains("gmail.modify"));
    }

    #[test]
    fn browser_open_returns_unavailable() {
        let err = host()
            .open_browser("https://accounts.google.com/o/oauth2/v2/auth")
            .unwrap_err();
        assert!(matches!(err, CoreError::PlatformUnavailable(_)));
        let message = err.to_string();
        assert!(message.contains("unavailable"));
    }
}
