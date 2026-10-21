//! macOS UI has not been touched. Backend only. The old UI was bad and was removed.
//! The UI still has to be built later.
//!
//! Credential adapter for the macOS Keychain. This crate does not touch SwiftUI.
//! On this host the system Keychain cannot be reached, so every operation fails
//! closed. Secrets are never written to a file and are never retained.

use desktop_core::account::AccountId;
use desktop_core::error::{CoreError, CoreResult};
use desktop_core::platform::{CredentialStore, SecureSecret};

const KEYCHAIN_UNAVAILABLE: &str = "macOS Keychain is not available on this host";

fn keychain_unavailable() -> CoreError {
    CoreError::PlatformUnavailable(KEYCHAIN_UNAVAILABLE.to_owned())
}

/// macOS Keychain credential store.
///
/// `store`, `load`, and `delete` return [`CoreError::PlatformUnavailable`]
/// because the macOS Keychain is not available on this host.
#[derive(Debug, Default)]
pub struct MacKeychain;

impl MacKeychain {
    pub fn new() -> Self {
        Self
    }
}

impl CredentialStore for MacKeychain {
    fn store(&self, _account: &AccountId, _key: &str, _secret: &SecureSecret) -> CoreResult<()> {
        Err(keychain_unavailable())
    }

    fn load(&self, _account: &AccountId, _key: &str) -> CoreResult<Option<SecureSecret>> {
        Err(keychain_unavailable())
    }

    fn delete(&self, _account: &AccountId, _key: &str) -> CoreResult<()> {
        Err(keychain_unavailable())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const SECRET: &[u8] = b"raw-keychain-secret-do-not-leak";

    fn account() -> AccountId {
        AccountId::new("acct-keychain").expect("account id")
    }

    fn secret() -> SecureSecret {
        SecureSecret::from_bytes(SECRET.to_vec())
    }

    #[test]
    fn store_fails_with_unavailable_error_and_hides_secret() {
        let store = MacKeychain::new();
        let secret = secret();
        let err = store
            .store(&account(), "refresh", &secret)
            .expect_err("store must fail when the macOS Keychain is unavailable");

        match &err {
            CoreError::PlatformUnavailable(message) => {
                assert_eq!(message, KEYCHAIN_UNAVAILABLE);
            }
            other => panic!("expected PlatformUnavailable, got {other:?}"),
        }

        let text = err.to_string();
        let secret_text = std::str::from_utf8(SECRET).expect("secret utf-8");
        assert!(text.contains(KEYCHAIN_UNAVAILABLE));
        assert!(!text.contains(secret_text));
        assert!(!format!("{err:?}").contains(secret_text));

        let rendered = format!("{secret:?}");
        assert_eq!(rendered, "SecureSecret([redacted])");
        assert!(!rendered.contains(secret_text));
        assert!(rendered.contains("redacted"));
    }

    #[test]
    fn load_and_delete_fail_with_the_same_unavailable_error() {
        let store = MacKeychain::new();
        let secret_text = std::str::from_utf8(SECRET).expect("secret utf-8");

        let load_err = store
            .load(&account(), "refresh")
            .expect_err("load must fail when the macOS Keychain is unavailable");
        let delete_err = store
            .delete(&account(), "refresh")
            .expect_err("delete must fail when the macOS Keychain is unavailable");

        for err in [&load_err, &delete_err] {
            match err {
                CoreError::PlatformUnavailable(message) => {
                    assert_eq!(message, KEYCHAIN_UNAVAILABLE);
                }
                other => panic!("expected PlatformUnavailable, got {other:?}"),
            }
            let text = err.to_string();
            assert!(!text.contains(secret_text));
            assert!(!format!("{err:?}").contains(secret_text));
        }
    }
}
