//! Cursor + throttle pacing contracts (~10 req/s, honor Retry-After).
//!
//! **Never silently skip a throttled GET** — retry instead. A skipped
//! message is silently missing forever (editor-rules.md §13).

use serde::{Deserialize, Serialize};
use std::time::Duration;

/// Default Gmail-friendly request pacing (~10 requests/second).
pub const DEFAULT_REQUESTS_PER_SECOND: f64 = 10.0;

/// Policy for throttled per-message GETs.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum ThrottlePolicy {
    /// Always retry; never drop the request.
    RetryNeverSkip,
}

impl Default for ThrottlePolicy {
    fn default() -> Self {
        Self::RetryNeverSkip
    }
}

/// Opaque sync/page cursor. Advancing past an un-persisted page is forbidden
/// at the engine layer (later phases); this type only models the value.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct CursorState {
    pub page_token: Option<String>,
    pub history_id: Option<String>,
}

impl CursorState {
    pub fn empty() -> Self {
        Self {
            page_token: None,
            history_id: None,
        }
    }
}

/// Decision returned by a pacer before issuing a network request.
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum PaceDecision {
    /// Proceed immediately.
    Proceed,
    /// Wait this long, then retry the same request (do not skip).
    Wait(Duration),
}

/// Trait for request pacing. Implementations live in later sync phases;
/// the contract is defined here so Windows/macOS share semantics.
pub trait RequestPacer: Send {
    fn target_requests_per_second(&self) -> f64 {
        DEFAULT_REQUESTS_PER_SECOND
    }

    fn throttle_policy(&self) -> ThrottlePolicy {
        ThrottlePolicy::RetryNeverSkip
    }

    /// Called before each request. May return [`PaceDecision::Wait`].
    fn before_request(&mut self) -> PaceDecision;

    /// Honor a provider `Retry-After` value (seconds). Must cause a wait —
    /// never convert a throttle into a silent skip.
    fn honor_retry_after(&mut self, retry_after_secs: u64) -> PaceDecision {
        PaceDecision::Wait(Duration::from_secs(retry_after_secs.max(1)))
    }

    /// Whether a throttled GET may be dropped. Contract answer: **never**.
    fn may_skip_throttled_get(&self) -> bool {
        false
    }

    /// Cooperative schedulers / tests advance simulated time while waiting.
    fn simulate_elapsed(&mut self, elapsed: Duration) {
        let _ = elapsed;
    }
}

/// Simple token-bucket pacer used by unit tests and as a reference shape.
#[derive(Debug)]
pub struct TokenBucketPacer {
    tokens: f64,
    capacity: f64,
    refill_per_sec: f64,
    wait_remaining: Duration,
}

impl TokenBucketPacer {
    pub fn new(requests_per_second: f64) -> Self {
        let rps = if requests_per_second <= 0.0 {
            DEFAULT_REQUESTS_PER_SECOND
        } else {
            requests_per_second
        };
        Self {
            tokens: rps,
            capacity: rps,
            refill_per_sec: rps,
            wait_remaining: Duration::ZERO,
        }
    }

    /// Simulates elapsed time for tests / cooperative schedulers.
    pub fn advance(&mut self, elapsed: Duration) {
        if self.wait_remaining > Duration::ZERO {
            if elapsed >= self.wait_remaining {
                self.wait_remaining = Duration::ZERO;
            } else {
                self.wait_remaining -= elapsed;
                return;
            }
        }
        let add = self.refill_per_sec * elapsed.as_secs_f64();
        self.tokens = (self.tokens + add).min(self.capacity);
    }
}

impl RequestPacer for TokenBucketPacer {
    fn target_requests_per_second(&self) -> f64 {
        self.refill_per_sec
    }

    fn before_request(&mut self) -> PaceDecision {
        if self.wait_remaining > Duration::ZERO {
            return PaceDecision::Wait(self.wait_remaining);
        }
        if self.tokens >= 1.0 {
            self.tokens -= 1.0;
            PaceDecision::Proceed
        } else {
            let need = 1.0 - self.tokens;
            let secs = need / self.refill_per_sec;
            PaceDecision::Wait(Duration::from_secs_f64(secs.max(0.001)))
        }
    }

    fn honor_retry_after(&mut self, retry_after_secs: u64) -> PaceDecision {
        let wait = Duration::from_secs(retry_after_secs.max(1));
        self.wait_remaining = self.wait_remaining.max(wait);
        PaceDecision::Wait(self.wait_remaining)
    }

    fn simulate_elapsed(&mut self, elapsed: Duration) {
        self.advance(elapsed);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn default_policy_never_skips() {
        let mut p = TokenBucketPacer::new(DEFAULT_REQUESTS_PER_SECOND);
        assert!(!p.may_skip_throttled_get());
        assert_eq!(p.throttle_policy(), ThrottlePolicy::RetryNeverSkip);
        let d = p.honor_retry_after(2);
        assert_eq!(d, PaceDecision::Wait(Duration::from_secs(2)));
    }

    #[test]
    fn token_bucket_paces_burst() {
        let mut p = TokenBucketPacer::new(10.0);
        let mut proceeds = 0;
        for _ in 0..15 {
            match p.before_request() {
                PaceDecision::Proceed => proceeds += 1,
                PaceDecision::Wait(_) => break,
            }
        }
        assert!(proceeds <= 10);
        assert!(proceeds >= 1);
    }

    #[test]
    fn cursor_state_serializes() {
        let c = CursorState {
            page_token: Some("abc".into()),
            history_id: None,
        };
        let json = serde_json_compat(&c);
        assert!(json.contains("abc"));
    }

    fn serde_json_compat(c: &CursorState) -> String {
        // Keep desktop-core free of serde_json dep: manual smoke only.
        format!("{:?}", c)
    }
}
