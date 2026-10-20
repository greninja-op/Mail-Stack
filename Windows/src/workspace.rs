//! Windows workspace façade over desktop-core (WinUI shell still pending).

use mailstack_desktop_core::mailbox::filter_destination;
use mailstack_desktop_core::{
    classify, demo_threads, display_fraction, search_threads, unified_inbox, AccountId,
    AccountRegistry, AccountSnapshot, CategoryGroup, Destination, MemoryMailboxStore, Privacy,
    StructuredQuery, SyncProgress, SyncStage, TokenBucket, BASE_SCOPES, REDIRECT_URI,
    ThreadSummary, base_scopes_exclude_modify,
};

pub struct WindowsWorkspace {
    pub registry: AccountRegistry,
    pub store: MemoryMailboxStore,
}

impl WindowsWorkspace {
    pub fn bootstrap_demo() -> Self {
        let registry = AccountRegistry::new();
        let store = MemoryMailboxStore::new();
        let id = AccountId::new("local-desktop-demo").unwrap();
        registry.upsert(AccountSnapshot {
            account_id: id.clone(),
            email: "demo@mailstack.local".into(),
            display_name: Some("Demo".into()),
            is_active: true,
        });
        store.replace_threads(&id, demo_threads());
        Self { registry, store }
    }

    pub fn inbox(&self) -> Vec<ThreadSummary> {
        let accounts: Vec<_> = self.registry.list().into_iter().map(|a| a.account_id).collect();
        let all = unified_inbox(&self.store, &accounts);
        filter_destination(&all, Destination::AllInbox)
    }

    pub fn oauth_contract_ok() -> bool {
        base_scopes_exclude_modify() && REDIRECT_URI.contains("oauth2redirect") && !BASE_SCOPES.is_empty()
    }

    pub fn privacy_ok() -> bool {
        !Privacy::should_fetch_remote_images()
    }

    pub fn sync_cap_ok() -> bool {
        let p = SyncProgress::Running {
            stage: SyncStage::Fetching,
            messages_processed: 100,
            total_estimate: Some(100),
        };
        (display_fraction(&p) - 0.99).abs() < f64::EPSILON
    }

    pub fn search_unread(&self) -> usize {
        let id = self.registry.active().unwrap();
        let threads = self.store.threads(&id);
        search_threads(
            &threads,
            &StructuredQuery {
                is_unread: Some(true),
                ..Default::default()
            },
        )
        .len()
    }

    pub fn classify_demo(&self) -> usize {
        let id = self.registry.active().unwrap();
        self.store
            .threads(&id)
            .iter()
            .filter(|t| matches!(classify(t), CategoryGroup::NeedsAttention))
            .count()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn workspace_demo_contracts() {
        let w = WindowsWorkspace::bootstrap_demo();
        assert!(!w.inbox().is_empty());
        assert!(WindowsWorkspace::oauth_contract_ok());
        assert!(WindowsWorkspace::privacy_ok());
        assert!(WindowsWorkspace::sync_cap_ok());
        assert!(w.search_unread() > 0);
        assert!(w.classify_demo() > 0);
        let mut b = TokenBucket::mailstack_default();
        assert!(b.try_take(0));
    }
}
