//! Windows shell entry (scaffold). Exercises shared desktop-core contracts.

use mailstack_desktop_core::mailbox::filter_destination;
use mailstack_desktop_core::{
    demo_threads, display_fraction, search_threads, Destination, Privacy, StructuredQuery,
    SyncProgress, SyncStage,
};

fn main() {
    let threads = demo_threads();
    println!("Mailstack Windows scaffold");
    println!("account: {}", threads[0].account_id.as_str());
    assert!(
        threads[0].account_id.is_demo(),
        "demo must never look like live Gmail sync target"
    );
    assert!(!Privacy::should_fetch_remote_images());

    let inbox = filter_destination(&threads, Destination::AllInbox);
    println!("inbox threads: {}", inbox.len());

    let q = StructuredQuery {
        is_unread: Some(true),
        ..Default::default()
    };
    let unread = search_threads(&threads, &q);
    println!("unread: {}", unread.len());

    let running = SyncProgress::Running {
        stage: SyncStage::Fetching,
        messages_processed: 100,
        total_estimate: Some(100),
    };
    let frac = display_fraction(&running);
    println!("sync display fraction (capped): {frac}");
    assert!((frac - 0.99).abs() < f64::EPSILON);
    println!("ok");
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn demo_smoke() {
        let threads = demo_threads();
        assert!(!threads.is_empty());
        assert!(threads[0].account_id.is_demo());
    }
}
