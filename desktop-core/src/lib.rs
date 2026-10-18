//! Shared desktop core — no OS UI. Consumed by macOS / Windows / Linux shells.

pub mod account;
pub mod mailbox;
pub mod pacing;
pub mod privacy;
pub mod search;
pub mod sync;

pub use account::{AccountId, AccountSnapshot};
pub use mailbox::{Destination, ThreadSummary, demo_threads};
pub use pacing::{PageCursor, SyncHttpError, TokenBucket};
pub use privacy::Privacy;
pub use search::{StructuredQuery, search_threads};
pub use sync::{SyncProgress, SyncStage, display_fraction};
