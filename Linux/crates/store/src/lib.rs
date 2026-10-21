//! Linux UI has not been touched. Backend only. Previous UI was removed because it was bad.
//!
//! Linux local store and OAuth host. Both sit on `desktop-core` contracts.
//! This crate does not open a window, link GTK, or launch a browser.

#![forbid(unsafe_code)]

use desktop_core::account::AccountId;
use desktop_core::error::{CoreError, CoreResult};
use desktop_core::oauth::{
    build_auth_url, default_scopes, generate_pkce, AuthUrlRequest, OAuthClientConfig, PkcePair,
};
use desktop_core::pacing::CursorState;
use desktop_core::persistence::{
    migrations, schema_forbids_tokens, AccountRecord, LocalStore, MemoryStore, StoredMessage,
};
use desktop_core::platform::BrowserOpen;

/// Returned when this host is asked to open a browser.
///
/// This backend crate has no Linux browser launcher. The error is the result;
/// no auth code, token, or connected account is produced instead.
const BROWSER_LAUNCHER_UNAVAILABLE: &str = "Linux browser launcher is not available here";

/// Linux local mailbox store.
///
/// This is in-memory only. There is no SQLite file backend in desktop-core yet.
/// [`MemoryStore`] is the store — this type does not open a database file and
/// does not invent a second database. Tokens are not written into stored rows.
/// [`schema_forbids_tokens`] is applied to every documented migration before
/// the store is constructed. Credentials stay in `CredentialStore`, which this
/// type does not hold.
pub struct LinuxLocalStore {
    inner: MemoryStore,
}

impl LinuxLocalStore {
    /// Builds an in-memory store after the documented schema passes the token audit.
    pub fn new() -> CoreResult<Self> {
        for migration in migrations() {
            if !schema_forbids_tokens(migration.sql) {
                return Err(CoreError::Internal(format!(
                    "migration {} names a token or secret column; refusing to store rows",
                    migration.name
                )));
            }
        }
        Ok(Self {
            inner: MemoryStore::new(),
        })
    }

    /// The in-memory desktop-core store. There is no file backend behind it.
    pub fn memory(&self) -> &MemoryStore {
        &self.inner
    }
}

impl LocalStore for LinuxLocalStore {
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

/// `schema_forbids_tokens` on the rendered stored row.
///
/// [`StoredMessage`] has no token column. This is the row-level check the
/// persistence API allows: the same audit used on schema SQL, applied to the
/// value that came back from [`MemoryStore`].
pub fn stored_row_forbids_tokens(message: &StoredMessage) -> bool {
    schema_forbids_tokens(&format!("{message:?}"))
}

/// Linux OAuth host.
///
/// Builds the Google authorization URL with PKCE S256 and [`default_scopes`].
/// The client config has a client id and redirect URI only. This host never
/// accepts a client secret, and opening a browser returns
/// [`CoreError::PlatformUnavailable`].
pub struct LinuxOAuthHost {
    client: OAuthClientConfig,
    pkce: PkcePair,
    state: String,
}

impl LinuxOAuthHost {
    /// Prepares a PKCE authorization request. `verifier_bytes` are the PKCE
    /// entropy; the verifier and challenge are derived by `desktop_core::oauth`.
    pub fn new(
        client: OAuthClientConfig,
        verifier_bytes: &[u8],
        state: impl Into<String>,
    ) -> Self {
        Self {
            client,
            pkce: generate_pkce(verifier_bytes),
            state: state.into(),
        }
    }

    pub fn pkce(&self) -> &PkcePair {
        &self.pkce
    }

    /// Authorization URL for the system browser. No client secret is included.
    pub fn auth_url(&self) -> String {
        let scopes = default_scopes()
            .into_iter()
            .map(str::to_owned)
            .collect();
        build_auth_url(&AuthUrlRequest {
            client: self.client.clone(),
            pkce: self.pkce.clone(),
            state: self.state.clone(),
            scopes,
            include_gmail_modify: false,
        })
    }

    /// Asks this host to open the authorization URL.
    ///
    /// Always returns [`CoreError::PlatformUnavailable`]. The Linux browser
    /// launcher is not available here. This does not return an auth code,
    /// a token, or a connected account.
    pub fn open_browser(&self) -> CoreResult<()> {
        Err(CoreError::PlatformUnavailable(
            BROWSER_LAUNCHER_UNAVAILABLE.into(),
        ))
    }
}

impl BrowserOpen for LinuxOAuthHost {
    fn open_url(&self, url: &str) -> CoreResult<()> {
        let _ = url;
        self.open_browser()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use desktop_core::oauth::SCOPE_GMAIL_READONLY;

    fn host() -> LinuxOAuthHost {
        LinuxOAuthHost::new(
            OAuthClientConfig::for_tests(
                "mailstack-linux-public-client",
                "http://127.0.0.1:1/oauth2redirect",
            ),
            b"abcdefghijklmnopqrstuvwxyz0123456789abcd",
            "linux-oauth-state",
        )
    }

    #[test]
    fn pkce_verifier_and_challenge_differ() {
        let host = host();
        let pkce = host.pkce();
        assert!(!pkce.verifier.is_empty());
        assert!(!pkce.challenge.is_empty());
        assert_ne!(pkce.verifier, pkce.challenge);
        let url = host.auth_url();
        assert!(url.contains(&pkce.challenge));
        assert!(!url.contains(&pkce.verifier));
    }

    #[test]
    fn auth_url_contains_gmail_readonly_scope_and_no_client_secret() {
        let url = host().auth_url();
        let encoded_scope = SCOPE_GMAIL_READONLY
            .replace(':', "%3A")
            .replace('/', "%2F");
        assert!(url.contains("gmail.readonly"));
        assert!(url.contains(&encoded_scope));
        assert!(!url.contains("client_secret"));
        assert!(!url.contains("client_secret="));
    }

    #[test]
    fn storing_a_message_does_not_persist_a_token_field() {
        for migration in migrations() {
            assert!(
                schema_forbids_tokens(migration.sql),
                "migration {} allows a token column",
                migration.name
            );
        }

        let store = LinuxLocalStore::new().expect("schema audit passed");
        let account_id = AccountId::new("linux-acct").unwrap();
        store
            .upsert_account(&AccountRecord {
                account_id: account_id.clone(),
                email: "reader@example.com".into(),
                display_name: "Reader".into(),
                is_demo: false,
                enabled: true,
            })
            .unwrap();

        let message = StoredMessage {
            account_id: account_id.clone(),
            message_id: "m1".into(),
            thread_id: "t1".into(),
            subject: "Hello from Linux".into(),
            snippet: "A short note".into(),
            sender_email: "sender@example.com".into(),
            sender_display_name: "Sender".into(),
            internal_date_ms: 1_700_000_000_000,
            is_unread: true,
            is_starred: false,
            inserted_at_ms: 1_700_000_000_100,
        };
        store
            .persist_page_then_cursor(&account_id, &[message.clone()], &CursorState::empty())
            .unwrap();

        let loaded = store.memory().recent_inserted(&account_id, 5).unwrap();
        assert_eq!(loaded.len(), 1);
        assert_eq!(loaded[0], message);
        assert!(stored_row_forbids_tokens(&loaded[0]));
        assert!(schema_forbids_tokens(&format!("{:?}", loaded[0])));
        let rendered = format!("{:?}", loaded[0]).to_ascii_lowercase();
        assert!(!rendered.contains("access_token"));
        assert!(!rendered.contains("refresh_token"));
        assert!(!rendered.contains("client_secret"));
        assert!(!rendered.contains("password"));
    }

    #[test]
    fn browser_open_returns_unavailable() {
        let host = host();
        let err = host.open_browser().expect_err("browser launcher is absent");
        match err {
            CoreError::PlatformUnavailable(message) => {
                assert_eq!(message, BROWSER_LAUNCHER_UNAVAILABLE);
            }
            other => panic!("expected platform unavailable, got {other:?}"),
        }

        let trait_err = BrowserOpen::open_url(&host, &host.auth_url())
            .expect_err("trait open must not launch a browser");
        let rendered = trait_err.to_string();
        assert!(rendered.contains(BROWSER_LAUNCHER_UNAVAILABLE));
        assert!(matches!(trait_err, CoreError::PlatformUnavailable(_)));
    }
}
