//! Sync request pacing for desktop platforms.
//!
//! Requirement (not Android behaviour): ~10 requests/sec token bucket,
//! honour Retry-After, retry dropped GETs instead of skipping them.
//! Android phone today has no token bucket — do not claim parity.

use std::time::Duration;

/// Simple token-bucket limiter. Capacity = burst; refill_per_sec ≈ sustained rate.
#[derive(Debug, Clone)]
pub struct TokenBucket {
    capacity: f64,
    tokens: f64,
    refill_per_sec: f64,
    /// Fake-clock friendly: milliseconds since an arbitrary epoch.
    last_ms: u64,
}

impl TokenBucket {
    pub fn new(capacity: f64, refill_per_sec: f64) -> Self {
        Self {
            capacity,
            tokens: capacity,
            refill_per_sec,
            last_ms: 0,
        }
    }

    /// Mailstack desktop default: ~10 req/sec with small burst.
    pub fn mailstack_default() -> Self {
        Self::new(10.0, 10.0)
    }

    pub fn try_take(&mut self, now_ms: u64) -> bool {
        self.refill(now_ms);
        if self.tokens >= 1.0 {
            self.tokens -= 1.0;
            true
        } else {
            false
        }
    }

    fn refill(&mut self, now_ms: u64) {
        if now_ms > self.last_ms {
            let elapsed_sec = (now_ms - self.last_ms) as f64 / 1000.0;
            self.tokens = (self.tokens + elapsed_sec * self.refill_per_sec).min(self.capacity);
            self.last_ms = now_ms;
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SyncHttpError {
    /// Per-second throttle — wait, then tap once.
    PerSecondThrottle { retry_after: Duration },
    /// Daily quota — fail-fast; resets at midnight Pacific.
    DailyQuota,
    Offline,
    Other(String),
}

impl SyncHttpError {
    pub fn from_status(status: u16, retry_after_secs: Option<u64>, body: &str) -> Self {
        let lower = body.to_lowercase();
        if lower.contains("daily") && (lower.contains("quota") || lower.contains("limit")) {
            return SyncHttpError::DailyQuota;
        }
        if status == 429
            || lower.contains("ratelimitexceeded")
            || lower.contains("userratelimit")
            || lower.contains("ratelimit")
        {
            return SyncHttpError::PerSecondThrottle {
                retry_after: Duration::from_secs(retry_after_secs.unwrap_or(60)),
            };
        }
        if status == 0 {
            return SyncHttpError::Offline;
        }
        SyncHttpError::Other(format!("HTTP {status}"))
    }

    pub fn user_message(&self) -> &'static str {
        match self {
            SyncHttpError::PerSecondThrottle { .. } => "wait a minute, tap once",
            SyncHttpError::DailyQuota => "resets at midnight Pacific",
            SyncHttpError::Offline => "You're offline — showing saved mail.",
            SyncHttpError::Other(_) => "Something went wrong. Try again.",
        }
    }

    pub fn is_reconnect_message(&self) -> bool {
        false // never say Reconnect for throttle/offline
    }
}

/// Dropped GET must be retried — never silently skipped. Cursor advances only after persist.
#[derive(Debug, Default)]
pub struct PageCursor {
    pub page_token: Option<String>,
    pub persisted: bool,
}

impl PageCursor {
    pub fn advance_after_persist(&mut self, next: Option<String>) {
        assert!(
            self.persisted,
            "cursor must not advance past an un-persisted page"
        );
        self.page_token = next;
        self.persisted = false;
    }

    pub fn mark_persisted(&mut self) {
        self.persisted = true;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bucket_paces_burst_then_refills() {
        let mut b = TokenBucket::new(2.0, 10.0);
        assert!(b.try_take(0));
        assert!(b.try_take(0));
        assert!(!b.try_take(0), "burst exhausted");
        // After 100ms at 10/sec → +1 token
        assert!(b.try_take(100));
    }

    #[test]
    fn retry_after_per_second_not_reconnect() {
        let e = SyncHttpError::from_status(429, Some(60), "rateLimitExceeded");
        assert!(matches!(e, SyncHttpError::PerSecondThrottle { .. }));
        assert_eq!(e.user_message(), "wait a minute, tap once");
        assert!(!e.is_reconnect_message());
    }

    #[test]
    fn daily_quota_fail_fast() {
        let e = SyncHttpError::from_status(403, None, "dailyLimitExceeded quota");
        assert_eq!(e, SyncHttpError::DailyQuota);
        assert_eq!(e.user_message(), "resets at midnight Pacific");
    }

    #[test]
    fn cursor_refuses_advance_without_persist() {
        let mut c = PageCursor::default();
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            c.advance_after_persist(Some("next".into()));
        }));
        assert!(result.is_err());
        c.mark_persisted();
        c.advance_after_persist(Some("next".into()));
        assert_eq!(c.page_token.as_deref(), Some("next"));
    }
}
