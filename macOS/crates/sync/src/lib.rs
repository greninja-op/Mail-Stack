//! macOS UI has not been touched. Backend only. The old UI was removed because it was bad, and a new UI still has to be built.
//!
//! Sync entry points for a future macOS app. Every rule below is a call into
//! `desktop-core` (`sync_engine`, `gmail`, `html`, `multi_account`). This crate
//! does not reimplement those rules, invent a sync percentage, or open a socket.

use desktop_core::{
    assert_may_sync, classify_sync_error, get_message_never_skip, sync_error_user_message,
    AccountId, AccountRecord, CancelToken, CoreError, CoreResult, GmailApi, GmailMessage,
    ImportFlowState, LocalStore, SanitizedHtml, SanitizePolicy, SyncCoordinator, SyncErrorClass,
    SyncProgressSnapshot, TokenBucketPacer, DEFAULT_REQUESTS_PER_SECOND,
};

/// Demo and local/seeded accounts are `is_demo` records. The core refuses them.
pub fn account_may_sync(account: &AccountRecord) -> CoreResult<()> {
    assert_may_sync(account)
}

/// Runs sync through [`SyncCoordinator`]. Progress is only the snapshot that
/// coordinator returns after a real run.
#[derive(Default)]
pub struct MacSync {
    coordinator: SyncCoordinator,
}

impl MacSync {
    pub fn new() -> Self {
        Self {
            coordinator: SyncCoordinator::new(),
        }
    }

    pub fn initial_sync<A: GmailApi, S: LocalStore>(
        &self,
        api: &A,
        store: &S,
        account: &AccountId,
        cancel: &CancelToken,
    ) -> CoreResult<ImportFlowState> {
        if let Some(record) = store.get_account(account)? {
            assert_may_sync(&record)?;
        }
        self.coordinator
            .run_initial_sync(api, store, account, cancel)
    }

    pub fn incremental_sync<A: GmailApi, S: LocalStore>(
        &self,
        api: &A,
        store: &S,
        account: &AccountId,
        cancel: &CancelToken,
    ) -> CoreResult<SyncProgressSnapshot> {
        if let Some(record) = store.get_account(account)? {
            assert_may_sync(&record)?;
        }
        self.coordinator
            .run_incremental_sync(api, store, account, cancel)
    }
}

/// Fetch one message. [`get_message_never_skip`] retries a throttled GET for
/// the same id. The `GmailApi` implementation performs any I/O; this crate
/// does not.
pub fn fetch_message<A: GmailApi>(
    api: &A,
    account: &AccountId,
    message_id: &str,
) -> CoreResult<GmailMessage> {
    let mut pacer = TokenBucketPacer::new(DEFAULT_REQUESTS_PER_SECOND);
    get_message_never_skip(api, &mut pacer, account, message_id, 20)
}

/// Sanitize a mail body with the core policy. Remote images are stripped, not fetched.
pub fn sanitize_body(raw: &str) -> SanitizedHtml {
    SanitizedHtml::from_untrusted(raw, SanitizePolicy::default())
}

/// Offline and sign-in stay separate classes. The offline sentence is the core's.
pub fn describe_sync_error(err: &CoreError) -> (SyncErrorClass, String) {
    (
        classify_sync_error(err),
        sync_error_user_message(err),
    )
}

#[cfg(test)]
mod tests {
    use super::*;
    use desktop_core::{
        AccountScoped, CursorState, GmailApiErrorKind, GmailMessage, MemoryStore, MockGmail,
    };

    fn record(id: &str, email: &str, is_demo: bool) -> AccountRecord {
        AccountRecord {
            account_id: AccountId::new(id).unwrap(),
            email: email.into(),
            display_name: id.into(),
            is_demo,
            enabled: true,
        }
    }

    fn message(id: &str) -> GmailMessage {
        GmailMessage {
            id: id.into(),
            thread_id: "t".into(),
            subject: "Hello".into(),
            snippet: "hi".into(),
            sender_email: "n@co.com".into(),
            sender_display: "N".into(),
            label_ids: vec!["INBOX".into()],
            internal_date_ms: 1,
            unread: true,
            starred: false,
            html_body: None,
        }
    }

    #[test]
    fn demo_and_local_accounts_do_not_sync() {
        let demo = record("fixture-demo", "demo@mailstack.local", true);
        let local = record("local-home", "local@mailstack.local", true);
        assert!(matches!(
            account_may_sync(&demo),
            Err(CoreError::InvalidArgument(_))
        ));
        assert!(matches!(
            account_may_sync(&local),
            Err(CoreError::InvalidArgument(_))
        ));

        let store = MemoryStore::new();
        store.upsert_account(&demo).unwrap();
        store.upsert_account(&local).unwrap();
        let api = MockGmail::with_fixture(0, vec![]);
        let sync = MacSync::new();
        let cancel = CancelToken::new();

        let demo_err = sync
            .initial_sync(&api, &store, demo.account_id(), &cancel)
            .unwrap_err();
        let local_err = sync
            .initial_sync(&api, &store, local.account_id(), &cancel)
            .unwrap_err();
        assert!(matches!(demo_err, CoreError::InvalidArgument(_)));
        assert!(matches!(local_err, CoreError::InvalidArgument(_)));
        assert!(store
            .recent_inserted(demo.account_id(), 10)
            .unwrap()
            .is_empty());
        assert!(store
            .recent_inserted(local.account_id(), 10)
            .unwrap()
            .is_empty());

        let incremental_err = sync
            .incremental_sync(&api, &store, local.account_id(), &cancel)
            .unwrap_err();
        assert!(matches!(incremental_err, CoreError::InvalidArgument(_)));
    }

    #[test]
    fn throttled_get_is_not_skipped() {
        let account = AccountId::new("gmail-1").unwrap();
        let mut api = MockGmail::with_fixture(1, vec![message("m1")]);
        api.throttle_gets = 2;
        let got = fetch_message(&api, &account, "m1").unwrap();
        assert_eq!(got.id, "m1");
        assert!(api.gets_attempted.load(std::sync::atomic::Ordering::SeqCst) >= 3);
    }

    #[test]
    fn sanitizer_drops_script_and_does_not_fetch_remote_images() {
        let raw = r#"Hello<script>alert(1)</script><img src="https://tracker.example/pixel.png"> world"#;
        let sanitized = sanitize_body(raw);
        let lower = sanitized.text.to_ascii_lowercase();
        assert!(!lower.contains("alert"));
        assert!(!lower.contains("<script"));
        assert!(sanitized.remote_images_blocked);
        assert!(!sanitized.text.contains("https://"));
        assert!(!sanitized.text.contains("http://"));
        assert!(!lower.contains("<img"));
        assert!(sanitized.text.contains("Hello"));
        assert!(sanitized.text.contains("world"));
    }

    #[test]
    fn offline_is_not_labeled_as_oauth_reconnect() {
        let offline = CoreError::from(GmailApiErrorKind::Offline);
        let auth = CoreError::from(GmailApiErrorKind::Auth);
        let (offline_class, offline_msg) = describe_sync_error(&offline);
        let (auth_class, auth_msg) = describe_sync_error(&auth);
        assert_eq!(offline_class, SyncErrorClass::Offline);
        assert_eq!(auth_class, SyncErrorClass::Auth);
        assert_ne!(offline_class, auth_class);
        assert!(offline_msg.to_ascii_lowercase().contains("offline"));
        assert!(!offline_msg.to_ascii_lowercase().contains("reconnect"));
        assert_ne!(offline_msg, auth_msg);
    }

    #[test]
    fn connected_account_progress_is_the_engine_snapshot() {
        let connected = record("gmail-1", "u@gmail.com", false);
        assert!(account_may_sync(&connected).is_ok());
        let store = MemoryStore::new();
        store.upsert_account(&connected).unwrap();
        let api = MockGmail::with_fixture(4, vec![message("m1")]);
        let state = MacSync::new()
            .initial_sync(
                &api,
                &store,
                connected.account_id(),
                &CancelToken::new(),
            )
            .unwrap();
        assert_eq!(state.progress.messages_total, 4);
        assert_eq!(state.progress.messages_processed, 1);
        assert!(state.progress.done);
        assert_eq!(state.progress.display_percent, 100);
        assert_eq!(state.continue_enabled, state.progress.done);
        assert_eq!(state.recent_inserted_ids, vec!["m1".to_string()]);

        store
            .persist_page_then_cursor(
                connected.account_id(),
                &[],
                &CursorState {
                    page_token: Some("page-2".into()),
                    history_id: Some("h1".into()),
                },
            )
            .unwrap();
        let snap = MacSync::new()
            .incremental_sync(
                &api,
                &store,
                connected.account_id(),
                &CancelToken::new(),
            )
            .unwrap();
        assert!(snap.done);
        assert_eq!(snap.display_percent, 100);
        assert_eq!(snap.messages_processed, 1);
    }
}
