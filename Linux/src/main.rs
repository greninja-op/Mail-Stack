//! Linux shell entry (scaffold). Validates shared desktop-core on Linux.

use mailstack_desktop_core::mailbox::filter_destination;
use mailstack_desktop_core::{
    demo_threads, display_fraction, search_threads, Destination, Privacy, StructuredQuery,
    SyncProgress, SyncStage,
};

fn main() {
    let threads = demo_threads();
    println!("Mailstack Linux scaffold");
    println!("account: {}", threads[0].account_id.as_str());
    assert!(threads[0].account_id.is_demo());
    assert!(!Privacy::should_fetch_remote_images());

    let starred = filter_destination(&threads, Destination::Starred);
    println!("starred: {}", starred.len());

    let q = StructuredQuery {
        free_text: Some("security".into()),
        ..Default::default()
    };
    let hits = search_threads(&threads, &q);
    println!("search 'security': {}", hits.len());
    assert!(!hits.is_empty());

    let done = SyncProgress::Succeeded {
        messages_processed: 42,
    };
    assert!((display_fraction(&done) - 1.0).abs() < f64::EPSILON);
    let _ = SyncStage::Idle;
    println!("ok");
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn privacy_and_demo() {
        assert!(!Privacy::should_fetch_remote_images());
        assert!(demo_threads()[0].account_id.is_demo());
    }
}
