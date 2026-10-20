//! Local persistence contracts. Platforms plug OS-specific stores; core owns schema semantics.

use crate::account::AccountId;
use crate::mailbox::ThreadSummary;
use std::collections::HashMap;
use std::sync::Mutex;

#[derive(Debug, Default)]
pub struct MemoryMailboxStore {
    inner: Mutex<HashMap<String, Vec<ThreadSummary>>>,
}

impl MemoryMailboxStore {
    pub fn new() -> Self {
        Self::default()
    }

    fn key(account: &AccountId) -> String {
        account.as_str().to_string()
    }

    pub fn replace_threads(&self, account: &AccountId, threads: Vec<ThreadSummary>) {
        self.inner.lock().unwrap().insert(Self::key(account), threads);
    }

    pub fn threads(&self, account: &AccountId) -> Vec<ThreadSummary> {
        self.inner
            .lock()
            .unwrap()
            .get(&Self::key(account))
            .cloned()
            .unwrap_or_default()
    }

    pub fn wipe_account(&self, account: &AccountId) {
        self.inner.lock().unwrap().remove(&Self::key(account));
    }

    pub fn wipe_all(&self) {
        self.inner.lock().unwrap().clear();
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::mailbox::demo_threads;

    #[test]
    fn account_isolation() {
        let store = MemoryMailboxStore::new();
        let a = AccountId::new("local-a").unwrap();
        let b = AccountId::new("local-b").unwrap();
        store.replace_threads(&a, demo_threads());
        assert!(!store.threads(&a).is_empty());
        assert!(store.threads(&b).is_empty());
        store.wipe_account(&a);
        assert!(store.threads(&a).is_empty());
    }
}
