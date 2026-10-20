//! Shared desktop core — no OS UI. Consumed by macOS / Windows / Linux shells.

pub mod account;
pub mod classification;
pub mod mailbox;
pub mod multi_account;
pub mod oauth;
pub mod pacing;
pub mod persistence;
pub mod privacy;
pub mod search;
pub mod sync;

pub use account::{AccountId, AccountSnapshot};
pub use classification::{CategoryGroup, classify, why as classification_why};
pub use mailbox::{Destination, ThreadSummary, demo_threads};
pub use multi_account::{AccountRegistry, unified_inbox};
pub use oauth::{BASE_SCOPES, PKCE_METHOD, REDIRECT_URI, base_scopes_exclude_modify};
pub use pacing::{PageCursor, SyncHttpError, TokenBucket};
pub use persistence::MemoryMailboxStore;
pub use privacy::Privacy;
pub use search::{StructuredQuery, search_threads};
pub use sync::{SyncProgress, SyncStage, display_fraction};
