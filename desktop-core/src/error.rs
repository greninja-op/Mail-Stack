//! Shared error types for desktop-core contracts.

use crate::account::AccountId;
use serde::{Deserialize, Serialize};
use thiserror::Error;

pub type CoreResult<T> = Result<T, CoreError>;

#[derive(Debug, Error, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum CoreError {
    #[error("account isolation violation: expected {expected}, got {actual}")]
    AccountIsolationViolation {
        expected: AccountId,
        actual: AccountId,
    },

    #[error("operation cancelled")]
    Cancelled,

    #[error("invalid argument: {0}")]
    InvalidArgument(String),

    #[error("not found: {0}")]
    NotFound(String),

    #[error("serialization boundary failure: {0}")]
    Serialization(String),

    #[error("platform service unavailable: {0}")]
    PlatformUnavailable(String),

    #[error("provider throttle: retry after {retry_after_secs}s")]
    Throttled { retry_after_secs: u64 },

    #[error("offline")]
    Offline,

    #[error("authentication required")]
    AuthenticationRequired,

    #[error("permission denied")]
    PermissionDenied,

    /// Gmail daily quota exhausted (distinct from per-second throttle).
    #[error("daily quota exceeded")]
    DailyQuota,

    #[error("internal: {0}")]
    Internal(String),
}

impl From<crate::cancel::Cancelled> for CoreError {
    fn from(_: crate::cancel::Cancelled) -> Self {
        CoreError::Cancelled
    }
}
