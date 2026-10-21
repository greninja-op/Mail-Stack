//! Linux UI has not been touched. This crate is backend only. The UI still has to be built; the old one was bad and was removed.
//!
//! Thin host over `desktop-core`. Initial sync, incremental sync, throttled
//! message fetch, HTML sanitizing, and failure copy all call the existing
//! functions. Nothing here reimplements sync or speaks Gmail HTTP.
//!
//! Not in `desktop-core`, and not invented here:
//! - a live Gmail HTTP client (`GmailApi` is a trait; `MockGmail` is the only type)
//! - an offline arm in `classify_provider_error` (401 is auth; other HTTP
//!   statuses become throttle, daily quota, permission, or `Other`)
//! - a local-account flag separate from `AccountRecord.is_demo`

#![forbid(unsafe_code)]

use desktop_core::{
    assert_may_sync, classify_sync_error, get_message_never_skip, sync_error_user_message,
    user_message_for_failure, AccountId, AccountRecord, CancelToken, Connectivity, CoreError,
    CoreResult, FailureClass, GmailApi, GmailMessage, ImportFlowState, LocalStore, RequestPacer,
    SanitizedHtml, SanitizePolicy, SyncCoordinator, SyncErrorClass, SyncProgressSnapshot,
};

/// Linux-side caller for the shared sync engine.
///
/// Progress values are whatever `SyncCoordinator` returns. This type does not
/// keep a percentage of its own.
#[derive(Default)]
pub struct LinuxSyncHost {
    coordinator: SyncCoordinator,
}

/// `classify_sync_error` plus the core's user string for that error.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct SyncFailureReport {
    pub class: SyncErrorClass,
    pub message: String,
}

impl LinuxSyncHost {
    pub fn new() -> Self {
        Self {
            coordinator: SyncCoordinator::new(),
        }
    }

    /// `SyncCoordinator::is_in_flight`.
    pub fn is_sync_in_flight(&self, account: &AccountId) -> bool {
        self.coordinator.is_in_flight(account)
    }

    /// `assert_may_sync`. Demo records (`is_demo`) are refused.
    ///
    /// `AccountRecord` has no separate local-account flag. The core treats the
    /// local demo mailbox as `is_demo`.
    pub fn account_may_sync(account: &AccountRecord) -> CoreResult<()> {
        assert_may_sync(account)
    }

    /// `SyncCoordinator::run_initial_sync`.
    ///
    /// A stored demo account returns `CoreError::InvalidArgument` and does not
    /// persist a page. The returned `ImportFlowState` is the engine snapshot.
    pub fn initial_sync<A: GmailApi, S: LocalStore>(
        &self,
        api: &A,
        store: &S,
        account: &AccountId,
        cancel: &CancelToken,
    ) -> CoreResult<ImportFlowState> {
        self.coordinator
            .run_initial_sync(api, store, account, cancel)
    }

    /// `SyncCoordinator::run_incremental_sync`.
    pub fn incremental_sync<A: GmailApi, S: LocalStore>(
        &self,
        api: &A,
        store: &S,
        account: &AccountId,
        cancel: &CancelToken,
    ) -> CoreResult<SyncProgressSnapshot> {
        self.coordinator
            .run_incremental_sync(api, store, account, cancel)
    }

    /// `get_message_never_skip`.
    ///
    /// Throttled gets are retried until `max_retries`, then the throttle error
    /// is returned. The id is not dropped.
    pub fn fetch_message<A: GmailApi, P: RequestPacer>(
        api: &A,
        pacer: &mut P,
        account: &AccountId,
        id: &str,
        max_retries: u32,
    ) -> CoreResult<GmailMessage> {
        get_message_never_skip(api, pacer, account, id, max_retries)
    }

    /// `SanitizedHtml::from_untrusted` with `SanitizePolicy::default`
    /// (strip scripts, block remote images, decode entities).
    pub fn sanitize_mail_html(raw: &str) -> SanitizedHtml {
        SanitizedHtml::from_untrusted(raw, SanitizePolicy::default())
    }

    /// Offline and auth stay different classes. Copy is `sync_error_user_message`.
    pub fn describe_sync_error(err: &CoreError) -> SyncFailureReport {
        SyncFailureReport {
            class: classify_sync_error(err),
            message: sync_error_user_message(err),
        }
    }

    /// `user_message_for_failure`. Offline connectivity is `FailureClass::Offline`.
    /// The reconnect sentence is the online authentication message.
    pub fn describe_connectivity_failure(connectivity: Connectivity, err: &CoreError) -> String {
        debug_assert_eq!(
            user_message_for_failure(connectivity, err),
            FailureClass::classify(connectivity, err).user_message()
        );
        user_message_for_failure(connectivity, err)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use desktop_core::{
        classify_provider_error, GmailApiErrorKind, GmailMessage, MemoryStore, MockGmail,
        TokenBucketPacer,
    };
    use std::sync::atomic::Ordering;

    fn message(id: &str) -> GmailMessage {
        GmailMessage {
            id: id.to_owned(),
            thread_id: "t".to_owned(),
            subject: "Hello".to_owned(),
            snippet: "hi".to_owned(),
            sender_email: "n@co.com".to_owned(),
            sender_display: "N".to_owned(),
            label_ids: vec!["INBOX".to_owned()],
            internal_date_ms: 1,
            unread: true,
            starred: false,
            html_body: None,
        }
    }

    fn upsert(store: &MemoryStore, id: &str, email: &str, is_demo: bool) -> AccountId {
        let account = AccountId::new(id).unwrap();
        store
            .upsert_account(&AccountRecord {
                account_id: account.clone(),
                email: email.to_owned(),
                display_name: id.to_owned(),
                is_demo,
                enabled: true,
            })
            .unwrap();
        account
    }

    #[test]
    fn demo_local_account_initial_sync_is_refused() {
        let store = MemoryStore::new();
        let account = upsert(&store, "demo", "demo@mailstack.local", true);
        let record = store.get_account(&account).unwrap().unwrap();
        let guard = LinuxSyncHost::account_may_sync(&record).unwrap_err();
        assert!(matches!(guard, CoreError::InvalidArgument(ref msg) if msg.contains("never sync")));

        let api = MockGmail::with_fixture(1, vec![message("m1")]);
        let host = LinuxSyncHost::new();
        let err = host
            .initial_sync(&api, &store, &account, &CancelToken::new())
            .unwrap_err();
        assert!(matches!(err, CoreError::InvalidArgument(ref msg) if msg.contains("demo")));
        assert!(store.recent_inserted(&account, 5).unwrap().is_empty());
        assert!(!host.is_sync_in_flight(&account));
    }

    #[test]
    fn demo_local_account_incremental_sync_is_refused() {
        let store = MemoryStore::new();
        let account = upsert(&store, "demo", "demo@mailstack.local", true);
        let api = MockGmail::with_fixture(1, vec![message("m1")]);
        let err = LinuxSyncHost::new()
            .incremental_sync(&api, &store, &account, &CancelToken::new())
            .unwrap_err();
        assert!(matches!(err, CoreError::InvalidArgument(_)));
        assert!(store.recent_inserted(&account, 5).unwrap().is_empty());
    }

    #[test]
    fn real_account_sync_returns_the_engine_snapshot() {
        let store = MemoryStore::new();
        let account = upsert(&store, "a", "u@gmail.com", false);
        let record = store.get_account(&account).unwrap().unwrap();
        LinuxSyncHost::account_may_sync(&record).unwrap();

        // Profile total is 4; the fixture page has one message. The host must
        // return those engine numbers. It does not compute 1/4.
        let api = MockGmail::with_fixture(4, vec![message("m1")]);
        let host = LinuxSyncHost::new();
        let state = host
            .initial_sync(&api, &store, &account, &CancelToken::new())
            .unwrap();
        assert!(state.continue_enabled);
        assert!(state.progress.done);
        assert_eq!(state.progress.messages_total, 4);
        assert_eq!(state.progress.messages_processed, 1);
        assert_eq!(state.progress.display_percent, 100);
        assert_eq!(state.recent_inserted_ids, vec!["m1".to_owned()]);
        assert_eq!(
            store.recent_inserted(&account, 5).unwrap()[0].message_id,
            "m1"
        );

        let snap = host
            .incremental_sync(&api, &store, &account, &CancelToken::new())
            .unwrap();
        assert!(snap.done);
        assert_eq!(snap.messages_processed, 1);
        assert_eq!(snap.display_percent, 100);
        assert!(!host.is_sync_in_flight(&account));
    }

    #[test]
    fn throttled_fetch_is_retried_not_skipped() {
        let account = AccountId::new("a").unwrap();
        let mut api = MockGmail::with_fixture(1, vec![message("m1")]);
        api.throttle_gets = 2;
        let mut pacer = TokenBucketPacer::new(100.0);
        assert!(!pacer.may_skip_throttled_get());

        let got = LinuxSyncHost::fetch_message(&api, &mut pacer, &account, "m1", 5).unwrap();
        assert_eq!(got.id, "m1");
        assert!(api.gets_attempted.load(Ordering::SeqCst) >= 3);
    }

    #[test]
    fn exhausted_throttle_is_an_error_not_a_skip() {
        let account = AccountId::new("a").unwrap();
        let mut api = MockGmail::with_fixture(1, vec![message("m1")]);
        api.throttle_gets = 5;
        let mut pacer = TokenBucketPacer::new(100.0);
        let err = LinuxSyncHost::fetch_message(&api, &mut pacer, &account, "m1", 1).unwrap_err();
        assert!(matches!(err, CoreError::Throttled { .. }));
        assert!(api.gets_attempted.load(Ordering::SeqCst) >= 2);
    }

    #[test]
    fn sanitizer_strips_script_and_remote_images() {
        // Default policy is the one the core ships: strip scripts, block remote images.
        let policy = SanitizePolicy::default();
        assert!(policy.strip_scripts);
        assert!(policy.block_remote_images);

        let script = LinuxSyncHost::sanitize_mail_html(r#"ok<script>alert(1)</script>done"#);
        let script_lower = script.text.to_ascii_lowercase();
        assert!(!script_lower.contains("alert"));
        assert!(!script_lower.contains("<script"));
        assert!(script.text.contains("ok"));
        assert!(script.text.contains("done"));

        let images = LinuxSyncHost::sanitize_mail_html(
            r#"Hello <img src="https://tracker.example/pixel.png"> <img src="http://evil.example/a.png"> world <img src="cid:logo">"#,
        );
        assert!(images.remote_images_blocked);
        assert!(!images.text.contains("https://"));
        assert!(!images.text.contains("http://"));
        assert!(images.text.contains("Hello"));
        assert!(images.text.contains("world"));
        // Non-remote cid images are kept. The core does not treat them as fetches.
        assert!(images.text.to_ascii_lowercase().contains("cid:logo"));

        // javascript: is rewritten to blocked:. The remainder of the URL stays as text.
        let js_url = LinuxSyncHost::sanitize_mail_html(r#"<a href="javascript:alert(1)">click</a>"#);
        assert!(!js_url.text.to_ascii_lowercase().contains("javascript:"));
        assert!(js_url.text.contains("blocked:"));

        let handlers = LinuxSyncHost::sanitize_mail_html(
            r#"<img src="cid:x" onerror="alert(1)"><a onclick="steal()">x</a>"#,
        );
        let handlers_lower = handlers.text.to_ascii_lowercase();
        assert!(!handlers_lower.contains("onerror"));
        assert!(!handlers_lower.contains("onclick"));
        assert!(!handlers_lower.contains("alert"));
        assert!(!handlers_lower.contains("steal"));
    }

    #[test]
    fn offline_sync_error_is_not_auth() {
        let offline = LinuxSyncHost::describe_sync_error(&CoreError::Offline);
        let auth = LinuxSyncHost::describe_sync_error(&CoreError::AuthenticationRequired);
        assert_eq!(offline.class, SyncErrorClass::Offline);
        assert_eq!(auth.class, SyncErrorClass::Auth);
        assert_eq!(offline.message, "You appear to be offline. Check the network.");
        assert_eq!(auth.message, "Sign-in required.");
        assert!(!offline.message.to_ascii_lowercase().contains("reconnect"));
        assert_ne!(offline.message, auth.message);
        assert_eq!(
            offline.message,
            sync_error_user_message(&CoreError::Offline)
        );
    }

    #[test]
    fn offline_connectivity_is_not_oauth_reconnect() {
        let offline = LinuxSyncHost::describe_connectivity_failure(
            Connectivity::Offline,
            &CoreError::Offline,
        );
        let oauth = LinuxSyncHost::describe_connectivity_failure(
            Connectivity::Online,
            &CoreError::AuthenticationRequired,
        );
        assert_eq!(
            offline,
            "You're offline. Changes will sync when you're back online."
        );
        assert_eq!(oauth, "Sign-in expired. Reconnect your Google account.");
        assert!(!offline.to_ascii_lowercase().contains("reconnect"));
        assert_ne!(offline, oauth);

        // Connectivity::Offline wins inside FailureClass::classify, including
        // when the error value is AuthenticationRequired.
        let still_offline = LinuxSyncHost::describe_connectivity_failure(
            Connectivity::Offline,
            &CoreError::AuthenticationRequired,
        );
        assert_eq!(still_offline, offline);
    }

    #[test]
    fn provider_classifier_has_no_offline_mapping() {
        // classify_provider_error does not return GmailApiErrorKind::Offline.
        // CoreError::Offline is classified only after something else built it.
        assert!(matches!(
            classify_provider_error(401, "unauthorized"),
            GmailApiErrorKind::Auth
        ));
        assert!(matches!(
            classify_provider_error(0, "connection refused"),
            GmailApiErrorKind::Other(_)
        ));
        assert_eq!(
            classify_sync_error(&CoreError::Offline),
            SyncErrorClass::Offline
        );
        assert_eq!(
            classify_sync_error(&CoreError::from(GmailApiErrorKind::Auth)),
            SyncErrorClass::Auth
        );
    }
}
