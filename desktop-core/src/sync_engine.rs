//! Phases 11–12 — initial + incremental sync orchestration.

use crate::account::AccountId;
use crate::cancel::CancelToken;
use crate::error::{CoreError, CoreResult};
use crate::gmail::{get_message_never_skip, GmailApi, GmailMessage};
use crate::pacing::TokenBucketPacer;
use crate::persistence::{LocalStore, StoredMessage};
use crate::sync::{SyncPhase, SyncProgress, SyncProgressSnapshot};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::{SystemTime, UNIX_EPOCH};

/// Import-flow UI model: arc pulse ≠ progress; ring uses [`SyncProgress`].
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct ImportFlowState {
    pub progress: SyncProgressSnapshot,
    /// Two most recently inserted messages (for the card rows).
    pub recent_inserted_ids: Vec<String>,
    pub continue_enabled: bool,
    pub error_message: Option<String>,
}

impl ImportFlowState {
    pub fn from_progress(progress: &SyncProgress, recent_ids: Vec<String>) -> Self {
        let snap = progress.snapshot();
        Self {
            continue_enabled: snap.done,
            progress: snap,
            recent_inserted_ids: recent_ids,
            error_message: None,
        }
    }

    pub fn with_error(mut self, err: &CoreError) -> Self {
        self.error_message = Some(sync_error_user_message(err));
        self.continue_enabled = false;
        self
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum SyncErrorClass {
    Throttle,
    DailyQuota,
    Permission,
    Offline,
    Auth,
    Other,
}

pub fn classify_sync_error(err: &CoreError) -> SyncErrorClass {
    match err {
        CoreError::Throttled { .. } => SyncErrorClass::Throttle,
        CoreError::DailyQuota => SyncErrorClass::DailyQuota,
        CoreError::Offline => SyncErrorClass::Offline,
        CoreError::AuthenticationRequired => SyncErrorClass::Auth,
        CoreError::PermissionDenied => SyncErrorClass::Permission,
        _ => SyncErrorClass::Other,
    }
}

/// Honest copy: offline ≠ "Reconnect".
pub fn sync_error_user_message(err: &CoreError) -> String {
    match classify_sync_error(err) {
        SyncErrorClass::Throttle => {
            "Gmail is rate-limiting requests. Wait a minute, then try once.".into()
        }
        SyncErrorClass::DailyQuota => {
            "Gmail daily quota reached. Try again after midnight Pacific.".into()
        }
        SyncErrorClass::Offline => "You appear to be offline. Check the network.".into(),
        SyncErrorClass::Auth => "Sign-in required.".into(),
        SyncErrorClass::Permission => "Additional Google permission is required.".into(),
        SyncErrorClass::Other => "Sync failed. Try again.".into(),
    }
}

struct InflightGuard {
    map: Arc<Mutex<HashMap<String, bool>>>,
    account: String,
}

impl Drop for InflightGuard {
    fn drop(&mut self) {
        if let Ok(mut g) = self.map.lock() {
            g.remove(&self.account);
        }
    }
}

/// Joins in-flight sync per account (no cancel/restart storm).
pub struct SyncCoordinator {
    inflight: Arc<Mutex<HashMap<String, bool>>>,
}

impl Default for SyncCoordinator {
    fn default() -> Self {
        Self {
            inflight: Arc::new(Mutex::new(HashMap::new())),
        }
    }
}

impl SyncCoordinator {
    pub fn new() -> Self {
        Self::default()
    }

    /// Returns true when a sync is already running for this account.
    pub fn is_in_flight(&self, account: &AccountId) -> bool {
        self.inflight
            .lock()
            .map(|g| g.contains_key(account.as_str()))
            .unwrap_or(false)
    }

    fn try_begin(&self, account: &AccountId) -> CoreResult<InflightGuard> {
        let mut g = self
            .inflight
            .lock()
            .map_err(|_| CoreError::Internal("lock".into()))?;
        if g.contains_key(account.as_str()) {
            // Join: do not cancel/restart — callers observe existing progress.
            return Err(CoreError::InvalidArgument(
                "sync already in flight for account".into(),
            ));
        }
        g.insert(account.as_str().to_owned(), true);
        Ok(InflightGuard {
            map: self.inflight.clone(),
            account: account.as_str().to_owned(),
        })
    }

    fn refuse_demo<S: LocalStore>(store: &S, account: &AccountId) -> CoreResult<()> {
        if let Some(rec) = store.get_account(account)? {
            if rec.is_demo {
                return Err(CoreError::InvalidArgument(
                    "demo accounts never sync".into(),
                ));
            }
        }
        Ok(())
    }

    /// Initial sync: profile total once → list → get (never skip) → persist → cursor.
    pub fn run_initial_sync<A: GmailApi, S: LocalStore>(
        &self,
        api: &A,
        store: &S,
        account: &AccountId,
        cancel: &CancelToken,
    ) -> CoreResult<ImportFlowState> {
        let _guard = self.try_begin(account)?;
        Self::refuse_demo(store, account)?;
        let profile = api.get_profile(account)?;
        let mut progress = SyncProgress::start(profile.messages_total);
        progress.set_phase(SyncPhase::Fetching);

        let mut cursor = store.load_cursor(account)?;
        let page = api.list_messages(account, cursor.page_token.as_deref())?;
        let mut pacer = TokenBucketPacer::new(10.0);
        let mut batch = Vec::new();
        let mut insert_base = now_ms();

        for refer in &page.messages {
            cancel.check()?;
            progress.set_phase(SyncPhase::Fetching);
            let msg = get_message_never_skip(api, &mut pacer, account, &refer.id, 20)?;
            batch.push(to_stored(account, &msg, insert_base));
            insert_base = insert_base.saturating_add(1);
            progress.add_processed(1);
            progress.set_phase(SyncPhase::Saving);
        }

        cursor.page_token = page.next_page_token.clone();
        cursor.history_id = Some(profile.history_id);
        store.persist_page_then_cursor(account, &batch, &cursor)?;
        progress.set_phase(SyncPhase::Finalizing);
        progress.mark_done();

        let recent = store.recent_inserted(account, 2)?;
        Ok(ImportFlowState::from_progress(
            &progress,
            recent.into_iter().map(|m| m.message_id).collect(),
        ))
    }

    /// Incremental: resume from saved cursor; same join semantics.
    pub fn run_incremental_sync<A: GmailApi, S: LocalStore>(
        &self,
        api: &A,
        store: &S,
        account: &AccountId,
        cancel: &CancelToken,
    ) -> CoreResult<SyncProgressSnapshot> {
        let _guard = self.try_begin(account)?;
        Self::refuse_demo(store, account)?;
        cancel.check()?;
        let cursor = store.load_cursor(account)?;
        let page = api.list_messages(account, cursor.page_token.as_deref())?;
        let mut progress = SyncProgress::start(page.messages.len() as u64);
        progress.set_phase(SyncPhase::Fetching);
        let mut pacer = TokenBucketPacer::new(10.0);
        let mut batch = Vec::new();
        let mut insert_base = now_ms();
        for refer in &page.messages {
            cancel.check()?;
            let msg = get_message_never_skip(api, &mut pacer, account, &refer.id, 20)?;
            batch.push(to_stored(account, &msg, insert_base));
            insert_base = insert_base.saturating_add(1);
            progress.add_processed(1);
        }
        let mut next = cursor;
        next.page_token = page.next_page_token;
        store.persist_page_then_cursor(account, &batch, &next)?;
        progress.mark_done();
        Ok(progress.snapshot())
    }
}

fn now_ms() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0)
}

fn to_stored(account: &AccountId, msg: &GmailMessage, inserted_at_ms: i64) -> StoredMessage {
    StoredMessage {
        account_id: account.clone(),
        message_id: msg.id.clone(),
        thread_id: msg.thread_id.clone(),
        subject: msg.subject.clone(),
        snippet: msg.snippet.clone(),
        sender_email: msg.sender_email.clone(),
        sender_display_name: msg.sender_display.clone(),
        internal_date_ms: msg.internal_date_ms,
        is_unread: msg.unread,
        is_starred: msg.starred,
        inserted_at_ms,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::gmail::{GmailMessage, MockGmail};
    use crate::pacing::CursorState;
    use crate::persistence::{AccountRecord, MemoryStore};

    #[test]
    fn initial_sync_progress_done_enables_continue() {
        let account = AccountId::new("a").unwrap();
        let store = MemoryStore::new();
        store
            .upsert_account(&AccountRecord {
                account_id: account.clone(),
                email: "u@gmail.com".into(),
                display_name: "U".into(),
                is_demo: false,
                enabled: true,
            })
            .unwrap();
        let api = MockGmail::with_fixture(
            1,
            vec![GmailMessage {
                id: "m1".into(),
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
            }],
        );
        let coord = SyncCoordinator::new();
        let state = coord
            .run_initial_sync(&api, &store, &account, &CancelToken::new())
            .unwrap();
        assert!(state.continue_enabled);
        assert_eq!(state.progress.display_percent, 100);
        assert_eq!(state.recent_inserted_ids, vec!["m1".to_string()]);
    }

    #[test]
    fn demo_accounts_never_sync() {
        let account = AccountId::new("demo").unwrap();
        let store = MemoryStore::new();
        store
            .upsert_account(&AccountRecord {
                account_id: account.clone(),
                email: "demo@mailstack.local".into(),
                display_name: "Demo".into(),
                is_demo: true,
                enabled: true,
            })
            .unwrap();
        let api = MockGmail::with_fixture(0, vec![]);
        let coord = SyncCoordinator::new();
        let err = coord
            .run_initial_sync(&api, &store, &account, &CancelToken::new())
            .unwrap_err();
        assert!(matches!(err, CoreError::InvalidArgument(_)));
    }

    #[test]
    fn offline_message_is_not_reconnect() {
        let msg = sync_error_user_message(&CoreError::Offline);
        assert!(!msg.to_ascii_lowercase().contains("reconnect"));
        assert!(msg.to_ascii_lowercase().contains("offline"));
    }

    #[test]
    fn daily_quota_message_distinct() {
        let msg = sync_error_user_message(&CoreError::DailyQuota);
        assert!(msg.to_ascii_lowercase().contains("quota"));
        assert!(msg.to_ascii_lowercase().contains("pacific"));
    }

    #[test]
    fn join_inflight_rejects_second() {
        let account = AccountId::new("a").unwrap();
        let coord = SyncCoordinator::new();
        let _g = coord.try_begin(&account).unwrap();
        assert!(coord.is_in_flight(&account));
        assert!(coord.try_begin(&account).is_err());
    }

    #[test]
    fn incremental_resumes_from_cursor() {
        let account = AccountId::new("a").unwrap();
        let store = MemoryStore::new();
        store
            .upsert_account(&AccountRecord {
                account_id: account.clone(),
                email: "u@gmail.com".into(),
                display_name: "U".into(),
                is_demo: false,
                enabled: true,
            })
            .unwrap();
        store
            .persist_page_then_cursor(
                &account,
                &[],
                &CursorState {
                    page_token: Some("page-2".into()),
                    history_id: Some("h1".into()),
                },
            )
            .unwrap();
        let msg = GmailMessage {
            id: "m2".into(),
            thread_id: "t".into(),
            subject: "Next".into(),
            snippet: "x".into(),
            sender_email: "n@co.com".into(),
            sender_display: "N".into(),
            label_ids: vec!["INBOX".into()],
            internal_date_ms: 2,
            unread: false,
            starred: false,
            html_body: None,
        };
        let api = MockGmail::with_fixture(1, vec![msg]);
        let coord = SyncCoordinator::new();
        let snap = coord
            .run_incremental_sync(&api, &store, &account, &CancelToken::new())
            .unwrap();
        assert!(snap.done);
        assert_eq!(store.recent_inserted(&account, 5).unwrap().len(), 1);
    }
}
