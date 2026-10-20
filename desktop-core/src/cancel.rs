//! Cooperative cancellation semantics for async desktop work.

use serde::{Deserialize, Serialize};
use std::sync::{
    atomic::{AtomicBool, Ordering},
    Arc,
};

/// Cooperative cancel flag shared across sync / IO tasks.
#[derive(Clone, Debug, Default)]
pub struct CancelToken {
    inner: Arc<AtomicBool>,
}

impl CancelToken {
    pub fn new() -> Self {
        Self {
            inner: Arc::new(AtomicBool::new(false)),
        }
    }

    pub fn cancel(&self) {
        self.inner.store(true, Ordering::SeqCst);
    }

    pub fn is_cancelled(&self) -> bool {
        self.inner.load(Ordering::SeqCst)
    }

    /// Returns [`Cancelled`] if the token was tripped.
    pub fn check(&self) -> Result<(), Cancelled> {
        if self.is_cancelled() {
            Err(Cancelled)
        } else {
            Ok(())
        }
    }
}

/// Distinct cancel signal (maps to [`crate::error::CoreError::Cancelled`]).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct Cancelled;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cancel_is_visible_to_clones() {
        let t = CancelToken::new();
        let t2 = t.clone();
        assert!(t.check().is_ok());
        t.cancel();
        assert!(t2.is_cancelled());
        assert!(t2.check().is_err());
    }
}
