use crate::account::{AccountId, AccountSnapshot};
use crate::mailbox::ThreadSummary;
use crate::persistence::MemoryMailboxStore;
use std::sync::Mutex;

#[derive(Debug, Default)]
pub struct AccountRegistry {
    accounts: Mutex<Vec<AccountSnapshot>>,
}

impl AccountRegistry {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn list(&self) -> Vec<AccountSnapshot> {
        self.accounts.lock().unwrap().clone()
    }

    pub fn upsert(&self, snap: AccountSnapshot) {
        let mut g = self.accounts.lock().unwrap();
        if let Some(i) = g.iter().position(|a| a.account_id == snap.account_id) {
            g[i] = snap;
        } else {
            g.push(snap);
        }
    }

    pub fn switch_active(&self, id: &AccountId) {
        let mut g = self.accounts.lock().unwrap();
        for a in g.iter_mut() {
            a.is_active = a.account_id == *id;
        }
    }

    pub fn active(&self) -> Option<AccountId> {
        self.accounts
            .lock()
            .unwrap()
            .iter()
            .find(|a| a.is_active)
            .map(|a| a.account_id.clone())
    }

    pub fn remove(&self, id: &AccountId, store: &MemoryMailboxStore) {
        self.accounts.lock().unwrap().retain(|a| a.account_id != *id);
        store.wipe_account(id);
    }
}

/// Unified inbox: merge threads but never mix account ownership on a row.
pub fn unified_inbox(store: &MemoryMailboxStore, accounts: &[AccountId]) -> Vec<ThreadSummary> {
    let mut out = Vec::new();
    for a in accounts {
        out.extend(store.threads(a));
    }
    out.sort_by_key(|t| std::cmp::Reverse(t.timestamp_epoch_ms));
    out
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::mailbox::demo_threads;

    #[test]
    fn switch_and_wipe_isolated() {
        let reg = AccountRegistry::new();
        let store = MemoryMailboxStore::new();
        let a = AccountId::new("local-a").unwrap();
        let b = AccountId::new("local-b").unwrap();
        reg.upsert(AccountSnapshot {
            account_id: a.clone(),
            email: "a@x".into(),
            display_name: None,
            is_active: true,
        });
        reg.upsert(AccountSnapshot {
            account_id: b.clone(),
            email: "b@x".into(),
            display_name: None,
            is_active: false,
        });
        let mut threads = demo_threads();
        for t in &mut threads {
            t.account_id = a.clone();
        }
        store.replace_threads(&a, threads);
        store.replace_threads(&b, vec![]);
        reg.switch_active(&b);
        assert_eq!(reg.active().unwrap().as_str(), "local-b");
        let unified = unified_inbox(&store, &[a.clone(), b.clone()]);
        assert!(unified.iter().all(|t| t.account_id == a));
        reg.remove(&a, &store);
        assert!(store.threads(&a).is_empty());
    }
}
