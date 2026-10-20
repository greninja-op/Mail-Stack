//! Sync progress contract (design.md / editor-rules.md).
//!
//! - Denominator (`messages_total`) is fetched **once** and never revised.
//! - While running, display percent caps at **99%**.
//! - **Done = 100%**.
//! - Progress never moves backwards.

use serde::{Deserialize, Serialize};

/// High-level sync phase labels for UI stage text (not percentages).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum SyncPhase {
    Connecting,
    Fetching,
    Saving,
    Processing,
    Finalizing,
    Done,
}

/// Mutable progress tracker enforcing honesty invariants.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct SyncProgress {
    messages_total: u64,
    messages_processed: u64,
    phase: SyncPhase,
    done: bool,
}

impl SyncProgress {
    /// Creates a tracker with a fixed denominator from Gmail `messagesTotal`.
    ///
    /// A zero total is allowed (empty mailbox): running still caps at 99%
    /// until [`mark_done`](Self::mark_done).
    pub fn start(messages_total: u64) -> Self {
        Self {
            messages_total,
            messages_processed: 0,
            phase: SyncPhase::Connecting,
            done: false,
        }
    }

    pub fn messages_total(&self) -> u64 {
        self.messages_total
    }

    pub fn messages_processed(&self) -> u64 {
        self.messages_processed
    }

    pub fn phase(&self) -> SyncPhase {
        self.phase
    }

    pub fn is_done(&self) -> bool {
        self.done
    }

    /// Sets the current phase. Ignored after done.
    pub fn set_phase(&mut self, phase: SyncPhase) {
        if self.done {
            return;
        }
        if phase != SyncPhase::Done {
            self.phase = phase;
        }
    }

    /// Advances processed count. Never decreases; ignored after done.
    pub fn set_processed(&mut self, processed: u64) {
        if self.done {
            return;
        }
        if processed > self.messages_processed {
            self.messages_processed = processed;
        }
    }

    /// Increments processed by `delta` (saturating). Never decreases.
    pub fn add_processed(&mut self, delta: u64) {
        if self.done {
            return;
        }
        self.messages_processed = self.messages_processed.saturating_add(delta);
    }

    /// Marks sync complete — display becomes 100%.
    pub fn mark_done(&mut self) {
        self.done = true;
        self.phase = SyncPhase::Done;
    }

    /// Display percent: 0–99 while running; 100 when done. Never exceeds 99
    /// until [`mark_done`](Self::mark_done), even if processed ≥ total.
    pub fn display_percent(&self) -> u8 {
        if self.done {
            return 100;
        }
        if self.messages_total == 0 {
            return 0;
        }
        let raw = ((self.messages_processed as f64) / (self.messages_total as f64) * 100.0).floor()
            as u64;
        raw.min(99) as u8
    }

    pub fn snapshot(&self) -> SyncProgressSnapshot {
        SyncProgressSnapshot {
            messages_total: self.messages_total,
            messages_processed: self.messages_processed,
            phase: self.phase,
            done: self.done,
            display_percent: self.display_percent(),
        }
    }
}

/// Serializable progress view for UI / persistence boundaries.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct SyncProgressSnapshot {
    pub messages_total: u64,
    pub messages_processed: u64,
    pub phase: SyncPhase,
    pub done: bool,
    pub display_percent: u8,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fixed_total_never_changes() {
        let mut p = SyncProgress::start(500);
        assert_eq!(p.messages_total(), 500);
        p.set_processed(100);
        assert_eq!(p.messages_total(), 500);
    }

    #[test]
    fn never_moves_backwards() {
        let mut p = SyncProgress::start(100);
        p.set_processed(40);
        p.set_processed(20);
        assert_eq!(p.messages_processed(), 40);
        p.add_processed(5);
        assert_eq!(p.messages_processed(), 45);
    }

    #[test]
    fn running_caps_at_99() {
        let mut p = SyncProgress::start(100);
        p.set_processed(100);
        assert_eq!(p.display_percent(), 99);
        p.set_processed(10_000);
        assert_eq!(p.display_percent(), 99);
    }

    #[test]
    fn done_is_100() {
        let mut p = SyncProgress::start(100);
        p.set_processed(50);
        p.mark_done();
        assert_eq!(p.display_percent(), 100);
        assert!(p.is_done());
        // Further updates ignored.
        p.set_processed(0);
        assert_eq!(p.messages_processed(), 50);
        assert_eq!(p.display_percent(), 100);
    }

    #[test]
    fn mid_progress_math() {
        let mut p = SyncProgress::start(200);
        p.set_processed(50);
        assert_eq!(p.display_percent(), 25);
    }
}
