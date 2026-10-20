//! Company intelligence + pin (≠ message star).
//!
//! Pinning moves companies to the top of that category's company filter.

use crate::account::{AccountId, AccountScoped};
use crate::category::CategoryGroupKind;
use crate::logo::{detect_sender_domain, SenderDomain};
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::sync::Mutex;

/// Stable company key within one account (usually canonical domain).
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct CompanyKey(String);

impl CompanyKey {
    pub fn new(raw: impl Into<String>) -> Option<Self> {
        let t = raw.into();
        let t = t.trim().to_ascii_lowercase();
        if t.is_empty() {
            None
        } else {
            Some(Self(t))
        }
    }

    pub fn as_str(&self) -> &str {
        &self.0
    }

    pub fn from_domain(domain: &SenderDomain) -> Self {
        Self(domain.as_str().to_owned())
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct CompanyRecord {
    pub account_id: AccountId,
    pub key: CompanyKey,
    pub display_name: String,
    pub user_override_name: Option<String>,
    /// Company pin — independent of message starring.
    pub pinned: bool,
}

impl AccountScoped for CompanyRecord {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

impl CompanyRecord {
    pub fn effective_name(&self) -> &str {
        self.user_override_name
            .as_deref()
            .unwrap_or(self.display_name.as_str())
    }
}

/// Resolve company key from sender identity (personal mailboxes → None).
pub fn company_key_from_sender(email: &str, display_name: &str) -> Option<CompanyKey> {
    detect_sender_domain(email, display_name).map(|d| CompanyKey::from_domain(&d))
}

/// In-memory company pin store (account-scoped).
#[derive(Default)]
pub struct CompanyPinStore {
    pins: Mutex<HashMap<(String, String), bool>>,
}

impl CompanyPinStore {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn set_pinned(
        &self,
        account: &AccountId,
        key: &CompanyKey,
        pinned: bool,
    ) -> Result<(), crate::error::CoreError> {
        let mut g = self
            .pins
            .lock()
            .map_err(|_| crate::error::CoreError::Internal("company pin lock".into()))?;
        g.insert((account.as_str().to_owned(), key.as_str().to_owned()), pinned);
        Ok(())
    }

    pub fn is_pinned(&self, account: &AccountId, key: &CompanyKey) -> bool {
        self.pins
            .lock()
            .ok()
            .and_then(|g| {
                g.get(&(account.as_str().to_owned(), key.as_str().to_owned()))
                    .copied()
            })
            .unwrap_or(false)
    }
}

/// Sort companies for a category filter: pinned first, then name.
pub fn sort_companies_for_filter(mut companies: Vec<CompanyRecord>) -> Vec<CompanyRecord> {
    companies.sort_by(|a, b| match (a.pinned, b.pinned) {
        (true, false) => std::cmp::Ordering::Less,
        (false, true) => std::cmp::Ordering::Greater,
        _ => a.effective_name().cmp(b.effective_name()),
    });
    companies
}

/// Filter helper: company keys present in a category's member set.
pub fn filter_keys_in_category(
    member_company_keys: &HashSet<CompanyKey>,
    category: CategoryGroupKind,
    _selected: CategoryGroupKind,
) -> bool {
    // Grouping is inside the selected category — caller passes pre-filtered members.
    let _ = category;
    !member_company_keys.is_empty()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pin_is_not_star() {
        let a = AccountId::new("a").unwrap();
        let key = CompanyKey::new("stripe.com").unwrap();
        let store = CompanyPinStore::new();
        store.set_pinned(&a, &key, true).unwrap();
        assert!(store.is_pinned(&a, &key));
        // Message star is a separate field on MessagePresentation — pin store
        // has no concept of message_id starring.
    }

    #[test]
    fn pinned_sort_first() {
        let a = AccountId::new("a").unwrap();
        let rows = vec![
            CompanyRecord {
                account_id: a.clone(),
                key: CompanyKey::new("zeta.com").unwrap(),
                display_name: "Zeta".into(),
                user_override_name: None,
                pinned: false,
            },
            CompanyRecord {
                account_id: a.clone(),
                key: CompanyKey::new("acme.com").unwrap(),
                display_name: "Acme".into(),
                user_override_name: None,
                pinned: true,
            },
        ];
        let sorted = sort_companies_for_filter(rows);
        assert_eq!(sorted[0].key.as_str(), "acme.com");
    }

    #[test]
    fn personal_gmail_has_no_company() {
        assert!(company_key_from_sender("a@gmail.com", "A").is_none());
    }
}
