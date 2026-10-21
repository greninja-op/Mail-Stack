//! Linux UI has not been touched. This file is backend only.
//!
//! Credential adapter for the Linux Secret Service (`org.freedesktop.secrets`).
//! This host has no Secret Service daemon. `store`, `load`, and `delete` return
//! [`CoreError::PlatformUnavailable`] and never write the secret to a file, a
//! log, memory fallback, or an `Ok` success.

use desktop_core::account::AccountId;
use desktop_core::error::{CoreError, CoreResult};
use desktop_core::platform::{CredentialStore, SecureSecret};

/// Shown by [`CoreError::PlatformUnavailable`]. Does not include secret material.
const SECRET_SERVICE_UNAVAILABLE: &str =
    "Linux secret service is not available on this host";

/// Linux Secret Service credential store.
///
/// No daemon is available here, so every operation fails. This type does not
/// wrap [`desktop_core::platform::MemoryCredentialStore`] and does not persist
/// secrets.
#[derive(Debug, Default)]
pub struct LinuxSecretService;

impl LinuxSecretService {
    pub fn new() -> Self {
        Self
    }

    fn unavailable(&self) -> CoreError {
        CoreError::PlatformUnavailable(SECRET_SERVICE_UNAVAILABLE.to_owned())
    }
}

impl CredentialStore for LinuxSecretService {
    fn store(&self, _account: &AccountId, _key: &str, _secret: &SecureSecret) -> CoreResult<()> {
        Err(self.unavailable())
    }

    fn load(&self, _account: &AccountId, _key: &str) -> CoreResult<Option<SecureSecret>> {
        Err(self.unavailable())
    }

    fn delete(&self, _account: &AccountId, _key: &str) -> CoreResult<()> {
        Err(self.unavailable())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE_SECRET: &[u8] = b"sample-secret-bytes-9f3c";

    fn account() -> AccountId {
        AccountId::new("acct-linux-keyring").expect("account id")
    }

    fn sample_secret() -> SecureSecret {
        SecureSecret::from_bytes(SAMPLE_SECRET.to_vec())
    }

    #[test]
    fn store_returns_unavailable_error() {
        let store = LinuxSecretService::new();
        let secret = sample_secret();
        let err = store
            .store(&account(), "refresh_token", &secret)
            .expect_err("store must not report success without Secret Service");

        match &err {
            CoreError::PlatformUnavailable(message) => {
                assert!(
                    message.contains("Linux secret service is not available on this host"),
                    "{message}"
                );
            }
            other => panic!("expected PlatformUnavailable, got {other:?}"),
        }

        let displayed = err.to_string();
        assert!(
            displayed.contains("Linux secret service is not available on this host"),
            "{displayed}"
        );

        let load_err = store
            .load(&account(), "refresh_token")
            .expect_err("load must not succeed without Secret Service");
        assert!(matches!(load_err, CoreError::PlatformUnavailable(_)));

        let delete_err = store
            .delete(&account(), "refresh_token")
            .expect_err("delete must not succeed without Secret Service");
        assert!(matches!(delete_err, CoreError::PlatformUnavailable(_)));
    }

    #[test]
    fn secure_secret_debug_does_not_print_secret_bytes() {
        let secret = sample_secret();
        let debug = format!("{secret:?}");
        let secret_text = std::str::from_utf8(SAMPLE_SECRET).expect("sample is utf-8");
        assert!(
            !debug.contains(secret_text),
            "Debug leaked secret bytes: {debug}"
        );
        assert!(
            debug.contains("redacted"),
            "Debug should mark the secret redacted: {debug}"
        );
    }

    #[test]
    fn sample_secret_is_absent_from_error_display() {
        let store = LinuxSecretService::new();
        let secret = sample_secret();
        let secret_text = std::str::from_utf8(SAMPLE_SECRET).expect("sample is utf-8");
        let err = store
            .store(&account(), "refresh_token", &secret)
            .expect_err("store must fail");

        let displayed = err.to_string();
        assert!(
            !displayed.contains(secret_text),
            "error Display leaked secret: {displayed}"
        );
        assert!(
            !format!("{err:?}").contains(secret_text),
            "error Debug leaked secret"
        );
        assert!(displayed.contains("Linux secret service is not available on this host"));
    }
}
