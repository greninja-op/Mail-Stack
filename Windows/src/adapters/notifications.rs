//! Phase 22 — Windows notification adapter with account-scoped dedup.

use desktop_core::background::NotificationDeduper;
use desktop_core::error::CoreResult;
use desktop_core::platform::{NotificationRequest, NotificationService};

/// Logs notification intent; real toast/WinRT wiring lands with WinAppSDK host.
/// Dedup keys are `(account_id, title+body hash proxy)` until message ids are wired.
#[derive(Default)]
pub struct WindowsNotificationService {
    pub last: std::sync::Mutex<Option<NotificationRequest>>,
    deduper: NotificationDeduper,
}

impl WindowsNotificationService {
    pub fn new() -> Self {
        Self::default()
    }

    /// Show only if `dedupe_key` has not already been surfaced for this account.
    pub fn show_deduped(&self, request: &NotificationRequest, dedupe_key: &str) -> CoreResult<bool> {
        if !self.deduper.should_show(&request.account_id, dedupe_key) {
            log::debug!(
                "notification deduped account={} key={}",
                request.account_id.as_str(),
                dedupe_key
            );
            return Ok(false);
        }
        self.show(request)?;
        Ok(true)
    }
}

impl NotificationService for WindowsNotificationService {
    fn show(&self, request: &NotificationRequest) -> CoreResult<()> {
        // Never log email addresses from body — lengths only.
        log::info!(
            "notification queued for account={} title_len={} body_len={}",
            request.account_id.as_str(),
            request.title.len(),
            request.body.len()
        );
        if let Ok(mut g) = self.last.lock() {
            *g = Some(request.clone());
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use desktop_core::account::AccountId;
    use desktop_core::platform::NotificationService;

    #[test]
    fn show_records_last() {
        let svc = WindowsNotificationService::default();
        let req = NotificationRequest {
            account_id: AccountId::new("a").unwrap(),
            title: "New mail".into(),
            body: "snippet".into(),
        };
        svc.show(&req).unwrap();
        assert!(svc.last.lock().unwrap().is_some());
    }

    #[test]
    fn dedup_suppresses_second() {
        let svc = WindowsNotificationService::new();
        let req = NotificationRequest {
            account_id: AccountId::new("a").unwrap(),
            title: "1 new message".into(),
            body: "Hello".into(),
        };
        assert!(svc.show_deduped(&req, "msg-1").unwrap());
        assert!(!svc.show_deduped(&req, "msg-1").unwrap());
    }
}
