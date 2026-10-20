//! Narrow Windows (and macOS) platform-service traits.
//!
//! Implementations live behind platform adapters — never in UI business logic.
//! Phase 02: stubs / trait contracts only.

use crate::account::AccountId;
use crate::error::{CoreError, CoreResult};
use serde::{Deserialize, Serialize};

/// Opaque secret bytes — never log or put in ordinary DB rows.
#[derive(Clone)]
pub struct SecureSecret(Vec<u8>);

impl SecureSecret {
    pub fn from_bytes(bytes: Vec<u8>) -> Self {
        Self(bytes)
    }

    pub fn as_bytes(&self) -> &[u8] {
        &self.0
    }

    pub fn expose_utf8(&self) -> Result<&str, CoreError> {
        std::str::from_utf8(&self.0)
            .map_err(|_| CoreError::InvalidArgument("secret is not utf-8".into()))
    }
}

impl std::fmt::Debug for SecureSecret {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str("SecureSecret([redacted])")
    }
}

impl Drop for SecureSecret {
    fn drop(&mut self) {
        for b in &mut self.0 {
            *b = 0;
        }
    }
}

/// Credential / token storage (Windows: Credential Manager / DPAPI adapter).
pub trait CredentialStore: Send + Sync {
    fn store(&self, account: &AccountId, key: &str, secret: &SecureSecret) -> CoreResult<()>;
    fn load(&self, account: &AccountId, key: &str) -> CoreResult<Option<SecureSecret>>;
    fn delete(&self, account: &AccountId, key: &str) -> CoreResult<()>;
}

/// Open a URL in the system browser (OAuth / safe external links).
pub trait BrowserOpen: Send + Sync {
    fn open_url(&self, url: &str) -> CoreResult<()>;
}

/// Toast / system notification request (no delivery in Phase 02).
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct NotificationRequest {
    pub account_id: AccountId,
    pub title: String,
    pub body: String,
}

/// Notification service stub trait.
pub trait NotificationService: Send + Sync {
    fn show(&self, request: &NotificationRequest) -> CoreResult<()>;
}

/// In-memory credential store for contract tests only — not for production.
#[derive(Default)]
pub struct MemoryCredentialStore {
    // (account, key) → bytes
    entries: std::sync::Mutex<std::collections::HashMap<(String, String), Vec<u8>>>,
}

impl CredentialStore for MemoryCredentialStore {
    fn store(&self, account: &AccountId, key: &str, secret: &SecureSecret) -> CoreResult<()> {
        let mut g = self
            .entries
            .lock()
            .map_err(|_| CoreError::Internal("credential lock poisoned".into()))?;
        g.insert(
            (account.as_str().to_owned(), key.to_owned()),
            secret.as_bytes().to_vec(),
        );
        Ok(())
    }

    fn load(&self, account: &AccountId, key: &str) -> CoreResult<Option<SecureSecret>> {
        let g = self
            .entries
            .lock()
            .map_err(|_| CoreError::Internal("credential lock poisoned".into()))?;
        Ok(g
            .get(&(account.as_str().to_owned(), key.to_owned()))
            .map(|b| SecureSecret::from_bytes(b.clone())))
    }

    fn delete(&self, account: &AccountId, key: &str) -> CoreResult<()> {
        let mut g = self
            .entries
            .lock()
            .map_err(|_| CoreError::Internal("credential lock poisoned".into()))?;
        g.remove(&(account.as_str().to_owned(), key.to_owned()));
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn memory_store_isolates_accounts() {
        let store = MemoryCredentialStore::default();
        let a = AccountId::new("a").unwrap();
        let b = AccountId::new("b").unwrap();
        store
            .store(&a, "refresh", &SecureSecret::from_bytes(b"tok-a".to_vec()))
            .unwrap();
        store
            .store(&b, "refresh", &SecureSecret::from_bytes(b"tok-b".to_vec()))
            .unwrap();
        let loaded_a = store.load(&a, "refresh").unwrap().unwrap();
        let loaded_b = store.load(&b, "refresh").unwrap().unwrap();
        assert_eq!(loaded_a.expose_utf8().unwrap(), "tok-a");
        assert_eq!(loaded_b.expose_utf8().unwrap(), "tok-b");
    }

    #[test]
    fn secure_secret_debug_redacts() {
        let s = SecureSecret::from_bytes(b"super-secret".to_vec());
        let dbg = format!("{:?}", s);
        assert!(!dbg.contains("super-secret"));
        assert!(dbg.contains("redacted"));
    }
}
