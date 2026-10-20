//! Offline / background work / notifications contracts (phase 22).
//!
//! Background work carries an explicit account id and never reads the active UI account.

use crate::account::{AccountId, AccountScoped};
use crate::error::{CoreError, CoreResult};
use crate::platform::NotificationRequest;
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::sync::Mutex;
use std::time::Duration;

/// Platform minimum clamp for periodic intervals (15 minutes).
pub const MIN_PERIODIC_INTERVAL: Duration = Duration::from_secs(15 * 60);
/// Lifecycle staleness threshold — no aggressive polling.
pub const STALENESS_THRESHOLD: Duration = Duration::from_secs(15 * 60);

/// Connectivity snapshot used by UI / schedulers.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum Connectivity {
    Online,
    Offline,
}

/// Offline taxonomy — keep auth / API / offline distinct so UI never says
/// "Reconnect" when the device is merely offline.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum FailureClass {
    Offline,
    Authentication,
    ApiThrottle { retry_after_secs: u64 },
    ApiError { message: String },
    Cancelled,
    Internal { message: String },
}

impl FailureClass {
    pub fn classify(connectivity: Connectivity, err: &CoreError) -> Self {
        match (connectivity, err) {
            (Connectivity::Offline, _) | (_, CoreError::Offline) => Self::Offline,
            (_, CoreError::AuthenticationRequired) => Self::Authentication,
            (_, CoreError::Throttled { retry_after_secs }) => Self::ApiThrottle {
                retry_after_secs: *retry_after_secs,
            },
            (_, CoreError::Cancelled) => Self::Cancelled,
            (_, CoreError::PermissionDenied) => Self::ApiError {
                message: "Permission denied".into(),
            },
            (_, other) => Self::Internal {
                message: other.to_string(),
            },
        }
    }

    /// Honest user copy — offline never prompts reconnect.
    pub fn user_message(&self) -> String {
        match self {
            Self::Offline => {
                "You're offline. Changes will sync when you're back online.".into()
            }
            Self::Authentication => "Sign-in expired. Reconnect your Google account.".into(),
            Self::ApiThrottle { .. } => "Gmail is busy. Wait a minute, then try once.".into(),
            Self::ApiError { message } => message.clone(),
            Self::Cancelled => "Cancelled.".into(),
            Self::Internal { message } => message.clone(),
        }
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct BackgroundWorkRequest {
    /// Explicit account — never inferred from active UI selection.
    pub account_id: AccountId,
    pub kind: BackgroundWorkKind,
    pub reason: String,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum BackgroundWorkKind {
    IncrementalSync,
    NotificationRefresh,
    IndexMaintenance,
}

impl AccountScoped for BackgroundWorkRequest {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

/// Clamp a requested periodic interval to the platform minimum.
pub fn clamp_periodic_interval(requested: Duration) -> Duration {
    requested.max(MIN_PERIODIC_INTERVAL)
}

/// Whether lifecycle-triggered work should run given last success time.
pub fn should_run_lifecycle_work(last_success_elapsed: Duration) -> bool {
    last_success_elapsed >= STALENESS_THRESHOLD
}

/// User-facing error copy: never say "Reconnect" when simply offline.
pub fn user_message_for_failure(connectivity: Connectivity, err: &CoreError) -> String {
    FailureClass::classify(connectivity, err).user_message()
}

/// Honest notification builder — no bait content.
pub fn build_notification(
    account_id: AccountId,
    unread_count: u32,
    latest_subject: Option<&str>,
) -> NotificationRequest {
    let title = if unread_count == 0 {
        "Mailstack".into()
    } else if unread_count == 1 {
        "1 new message".into()
    } else {
        format!("{unread_count} new messages")
    };
    let body = latest_subject.unwrap_or("Open Mailstack to read.").to_owned();
    NotificationRequest {
        account_id,
        title,
        body,
    }
}

/// Deduplicate notifications by account + stable key (message id / digest).
#[derive(Default)]
pub struct NotificationDeduper {
    seen: Mutex<HashMap<String, HashSet<String>>>,
}

impl NotificationDeduper {
    pub fn new() -> Self {
        Self::default()
    }

    fn map_key(account: &AccountId) -> String {
        account.as_str().to_owned()
    }

    /// Returns `true` if this key has not been shown yet for the account.
    pub fn should_show(&self, account: &AccountId, dedupe_key: &str) -> bool {
        let Ok(mut g) = self.seen.lock() else {
            return true;
        };
        let set = g.entry(Self::map_key(account)).or_default();
        if set.contains(dedupe_key) {
            false
        } else {
            set.insert(dedupe_key.to_owned());
            true
        }
    }

    pub fn forget_account(&self, account: &AccountId) {
        if let Ok(mut g) = self.seen.lock() {
            g.remove(&Self::map_key(account));
        }
    }
}

/// Trait for scheduling platform background work.
pub trait BackgroundScheduler: Send + Sync {
    fn enqueue(&self, work: &BackgroundWorkRequest) -> CoreResult<()>;
}

/// In-memory scheduler for tests / interim Windows wiring.
#[derive(Default)]
pub struct MemoryBackgroundScheduler {
    pub queue: Mutex<Vec<BackgroundWorkRequest>>,
}

impl BackgroundScheduler for MemoryBackgroundScheduler {
    fn enqueue(&self, work: &BackgroundWorkRequest) -> CoreResult<()> {
        // Explicit account required — reject empty (AccountId construction already does).
        self.queue
            .lock()
            .map_err(|_| CoreError::Internal("scheduler lock poisoned".into()))?
            .push(work.clone());
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn clamps_interval_to_15m() {
        assert_eq!(
            clamp_periodic_interval(Duration::from_secs(60)),
            MIN_PERIODIC_INTERVAL
        );
    }

    #[test]
    fn staleness_requires_15m() {
        assert!(!should_run_lifecycle_work(Duration::from_secs(60)));
        assert!(should_run_lifecycle_work(STALENESS_THRESHOLD));
    }

    #[test]
    fn offline_message_not_reconnect() {
        let msg = user_message_for_failure(Connectivity::Offline, &CoreError::Offline);
        assert!(!msg.to_ascii_lowercase().contains("reconnect"));
        assert!(msg.to_ascii_lowercase().contains("offline"));
        let auth = user_message_for_failure(
            Connectivity::Online,
            &CoreError::AuthenticationRequired,
        );
        assert!(auth.to_ascii_lowercase().contains("reconnect"));
    }

    #[test]
    fn taxonomy_separates_offline_and_auth() {
        assert_eq!(
            FailureClass::classify(Connectivity::Offline, &CoreError::AuthenticationRequired),
            FailureClass::Offline
        );
        assert_eq!(
            FailureClass::classify(Connectivity::Online, &CoreError::AuthenticationRequired),
            FailureClass::Authentication
        );
    }

    #[test]
    fn work_requires_explicit_account() {
        let w = BackgroundWorkRequest {
            account_id: AccountId::new("a").unwrap(),
            kind: BackgroundWorkKind::IncrementalSync,
            reason: "lifecycle".into(),
        };
        assert_eq!(w.account_id().as_str(), "a");
    }

    #[test]
    fn notification_dedup_per_account() {
        let d = NotificationDeduper::new();
        let a = AccountId::new("a").unwrap();
        let b = AccountId::new("b").unwrap();
        assert!(d.should_show(&a, "msg-1"));
        assert!(!d.should_show(&a, "msg-1"));
        assert!(d.should_show(&b, "msg-1"));
    }

    #[test]
    fn scheduler_keeps_explicit_account() {
        let s = MemoryBackgroundScheduler::default();
        let w = BackgroundWorkRequest {
            account_id: AccountId::new("acct-x").unwrap(),
            kind: BackgroundWorkKind::NotificationRefresh,
            reason: "test".into(),
        };
        s.enqueue(&w).unwrap();
        let q = s.queue.lock().unwrap();
        assert_eq!(q[0].account_id.as_str(), "acct-x");
    }
}
