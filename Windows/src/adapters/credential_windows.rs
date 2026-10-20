//! Phase 08 — Windows Credential Manager / DPAPI-backed adapter.
//!
//! Uses CredWriteW / CredReadW / CredDeleteW when built for Windows.
//! On failure, falls back to [`MemoryCredentialStore`] (tests / restricted
//! environments). Tokens never enter the local DB.

use desktop_core::account::AccountId;
use desktop_core::error::{CoreError, CoreResult};
use desktop_core::platform::{CredentialStore, MemoryCredentialStore, SecureSecret};

const TARGET_PREFIX: &str = "Mailstack/";

/// Common secret keys (never logged).
pub const KEY_REFRESH_TOKEN: &str = "refresh_token";
pub const KEY_ACCESS_TOKEN: &str = "access_token";
pub const KEY_PENDING_PKCE: &str = "pending_pkce";

pub struct WindowsCredentialStore {
    fallback: MemoryCredentialStore,
    /// When false, skip Win32 Cred* and use memory only (tests).
    pub use_wincred: bool,
}

impl Default for WindowsCredentialStore {
    fn default() -> Self {
        Self::new()
    }
}

impl WindowsCredentialStore {
    pub fn new() -> Self {
        Self {
            fallback: MemoryCredentialStore::default(),
            use_wincred: cfg!(windows),
        }
    }

    /// Force memory-only mode (unit tests).
    pub fn memory_only() -> Self {
        Self {
            fallback: MemoryCredentialStore::default(),
            use_wincred: false,
        }
    }

    fn target(account: &AccountId, key: &str) -> String {
        format!("{TARGET_PREFIX}{}/{key}", account.as_str())
    }

    /// Logout / disconnect: wipe known token keys for an account.
    pub fn clear_account_secrets(&self, account: &AccountId) -> CoreResult<()> {
        for key in [KEY_REFRESH_TOKEN, KEY_ACCESS_TOKEN, KEY_PENDING_PKCE] {
            self.delete(account, key)?;
        }
        Ok(())
    }
}

impl CredentialStore for WindowsCredentialStore {
    fn store(&self, account: &AccountId, key: &str, secret: &SecureSecret) -> CoreResult<()> {
        #[cfg(windows)]
        if self.use_wincred {
            match wincred_write(&Self::target(account, key), secret.as_bytes()) {
                Ok(()) => return Ok(()),
                Err(e) => log::warn!("CredWrite failed ({e}); memory fallback"),
            }
        }
        self.fallback.store(account, key, secret)
    }

    fn load(&self, account: &AccountId, key: &str) -> CoreResult<Option<SecureSecret>> {
        #[cfg(windows)]
        if self.use_wincred {
            match wincred_read(&Self::target(account, key)) {
                Ok(Some(bytes)) => return Ok(Some(SecureSecret::from_bytes(bytes))),
                Ok(None) => {}
                Err(e) => log::warn!("CredRead failed ({e}); memory fallback"),
            }
        }
        self.fallback.load(account, key)
    }

    fn delete(&self, account: &AccountId, key: &str) -> CoreResult<()> {
        #[cfg(windows)]
        if self.use_wincred {
            let _ = wincred_delete(&Self::target(account, key));
        }
        self.fallback.delete(account, key)
    }
}

#[cfg(windows)]
fn wide_null(s: &str) -> Vec<u16> {
    use std::os::windows::ffi::OsStrExt;
    std::ffi::OsStr::new(s)
        .encode_wide()
        .chain(std::iter::once(0))
        .collect()
}

#[cfg(windows)]
fn wincred_write(target: &str, bytes: &[u8]) -> CoreResult<()> {
    use windows::core::PWSTR;
    use windows::Win32::Security::Credentials::{
        CredWriteW, CREDENTIALW, CRED_PERSIST_LOCAL_MACHINE, CRED_TYPE_GENERIC,
    };

    let mut target_w = wide_null(target);
    let mut blob = bytes.to_vec();
    let cred = CREDENTIALW {
        Flags: Default::default(),
        Type: CRED_TYPE_GENERIC,
        TargetName: PWSTR(target_w.as_mut_ptr()),
        Comment: PWSTR::null(),
        LastWritten: Default::default(),
        CredentialBlobSize: blob.len() as u32,
        CredentialBlob: blob.as_mut_ptr(),
        Persist: CRED_PERSIST_LOCAL_MACHINE,
        AttributeCount: 0,
        Attributes: std::ptr::null_mut(),
        TargetAlias: PWSTR::null(),
        UserName: PWSTR::null(),
    };
    let result = unsafe { CredWriteW(&cred, 0) };
    for b in &mut blob {
        *b = 0;
    }
    result.map_err(|e| CoreError::Internal(format!("CredWriteW: {e}")))
}

#[cfg(windows)]
fn wincred_read(target: &str) -> CoreResult<Option<Vec<u8>>> {
    use windows::core::PCWSTR;
    use windows::Win32::Foundation::ERROR_NOT_FOUND;
    use windows::Win32::Security::Credentials::{
        CredFree, CredReadW, CREDENTIALW, CRED_TYPE_GENERIC,
    };

    let target_w = wide_null(target);
    let mut pcred: *mut CREDENTIALW = std::ptr::null_mut();
    match unsafe { CredReadW(PCWSTR(target_w.as_ptr()), CRED_TYPE_GENERIC, 0, &mut pcred) } {
        Ok(()) => {
            if pcred.is_null() {
                return Ok(None);
            }
            let cred = unsafe { &*pcred };
            let slice = unsafe {
                std::slice::from_raw_parts(cred.CredentialBlob, cred.CredentialBlobSize as usize)
            };
            let bytes = slice.to_vec();
            unsafe {
                CredFree(pcred as *const std::ffi::c_void);
            }
            Ok(Some(bytes))
        }
        Err(e) => {
            if e.code() == ERROR_NOT_FOUND.to_hresult() {
                Ok(None)
            } else {
                Err(CoreError::Internal(format!("CredReadW: {e}")))
            }
        }
    }
}

#[cfg(windows)]
fn wincred_delete(target: &str) -> CoreResult<()> {
    use windows::core::PCWSTR;
    use windows::Win32::Security::Credentials::{CredDeleteW, CRED_TYPE_GENERIC};

    let target_w = wide_null(target);
    unsafe { CredDeleteW(PCWSTR(target_w.as_ptr()), CRED_TYPE_GENERIC, 0) }
        .map_err(|e| CoreError::Internal(format!("CredDeleteW: {e}")))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn memory_fallback_roundtrip() {
        let store = WindowsCredentialStore::memory_only();
        let acct = AccountId::new("t1").unwrap();
        store
            .store(
                &acct,
                KEY_REFRESH_TOKEN,
                &SecureSecret::from_bytes(b"tok".to_vec()),
            )
            .unwrap();
        let loaded = store.load(&acct, KEY_REFRESH_TOKEN).unwrap().unwrap();
        assert_eq!(loaded.expose_utf8().unwrap(), "tok");
        store.clear_account_secrets(&acct).unwrap();
        assert!(store.load(&acct, KEY_REFRESH_TOKEN).unwrap().is_none());
    }
}
