//! Explicit account isolation boundary.
//!
//! Every account-owned value must carry an [`AccountId`]. Cross-account
//! mixing is a contract violation — never treat sender identity as the
//! receiving account.

use serde::{Deserialize, Serialize};
use std::fmt;

/// Opaque, non-empty account identifier. Distinct accounts never share ids.
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct AccountId(String);

impl AccountId {
    /// Creates an account id. Empty / whitespace-only strings are rejected.
    pub fn new(raw: impl Into<String>) -> Option<Self> {
        let s = raw.into();
        let trimmed = s.trim();
        if trimmed.is_empty() {
            None
        } else {
            Some(Self(trimmed.to_owned()))
        }
    }

    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Display for AccountId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

/// Marker trait: types that are owned by exactly one account.
pub trait AccountScoped {
    fn account_id(&self) -> &AccountId;
}

/// Returns true only when both sides refer to the same account.
pub fn same_account(a: &AccountId, b: &AccountId) -> bool {
    a == b
}

/// Asserts that a value belongs to `expected`. Returns `Err` on mismatch.
pub fn require_account(
    expected: &AccountId,
    actual: &AccountId,
) -> Result<(), crate::error::CoreError> {
    if expected == actual {
        Ok(())
    } else {
        Err(crate::error::CoreError::AccountIsolationViolation {
            expected: expected.clone(),
            actual: actual.clone(),
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_empty_account_id() {
        assert!(AccountId::new("").is_none());
        assert!(AccountId::new("   ").is_none());
    }

    #[test]
    fn distinct_accounts_are_not_equal() {
        let a = AccountId::new("acct-a").unwrap();
        let b = AccountId::new("acct-b").unwrap();
        assert!(!same_account(&a, &b));
        assert!(require_account(&a, &b).is_err());
        assert!(require_account(&a, &a).is_ok());
    }

    #[test]
    fn trims_whitespace_but_preserves_inner() {
        let a = AccountId::new("  user@example.com  ").unwrap();
        assert_eq!(a.as_str(), "user@example.com");
    }
}
