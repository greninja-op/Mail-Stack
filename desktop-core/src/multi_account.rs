//! Multi-account unified inbox (presentation-layer join only).
//!
//! Every unified row shows the receiving-account indicator (not sender).
//! Demo/seeded accounts never sync.

use crate::account::{AccountId, AccountScoped};
use crate::error::{CoreError, CoreResult};
use crate::mailbox::MessagePresentation;
use crate::persistence::{AccountRecord, LocalStore};
use serde::{Deserialize, Serialize};

/// Receiving-account chip for unified rows.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReceivingAccountIndicator {
    pub account_id: AccountId,
    pub email: String,
    pub short_label: String,
}

impl ReceivingAccountIndicator {
    pub fn from_account(account: &AccountRecord) -> Self {
        let short_label = account
            .email
            .split('@')
            .next()
            .unwrap_or(account.email.as_str())
            .chars()
            .take(12)
            .collect();
        Self {
            account_id: account.account_id.clone(),
            email: account.email.clone(),
            short_label,
        }
    }
}

/// Unified inbox row — presentation only; ownership stays per-account.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct UnifiedInboxRow {
    pub message: MessagePresentation,
    pub receiving: ReceivingAccountIndicator,
}

impl AccountScoped for UnifiedInboxRow {
    fn account_id(&self) -> &AccountId {
        &self.message.account_id
    }
}

/// Join messages from enabled accounts. Never mixes ownership across accounts.
pub fn unified_inbox_join(
    store: &dyn LocalStore,
    per_account_messages: &[(AccountId, Vec<MessagePresentation>)],
    limit: usize,
) -> CoreResult<Vec<UnifiedInboxRow>> {
    let accounts = store.list_accounts()?;
    let enabled: std::collections::HashMap<_, _> = accounts
        .into_iter()
        .filter(|a| a.enabled)
        .map(|a| (a.account_id.clone(), a))
        .collect();

    let mut rows = Vec::new();
    for (acct, msgs) in per_account_messages {
        let Some(rec) = enabled.get(acct) else {
            continue;
        };
        let indicator = ReceivingAccountIndicator::from_account(rec);
        for m in msgs {
            crate::account::require_account(acct, &m.account_id)?;
            rows.push(UnifiedInboxRow {
                message: m.clone(),
                receiving: indicator.clone(),
            });
        }
    }
    rows.sort_by(|a, b| {
        b.message
            .internal_date_ms
            .cmp(&a.message.internal_date_ms)
    });
    rows.truncate(limit);
    Ok(rows)
}

/// Guard used by sync: demo accounts must not sync.
pub fn assert_may_sync(account: &AccountRecord) -> CoreResult<()> {
    if account.is_demo {
        Err(CoreError::InvalidArgument(
            "demo/seeded accounts never sync".into(),
        ))
    } else {
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::mailbox::MessagePresentation;
    use crate::persistence::MemoryStore;

    #[test]
    fn demo_never_syncs() {
        let a = AccountRecord {
            account_id: AccountId::new("d").unwrap(),
            email: "demo@ex.com".into(),
            display_name: "Demo".into(),
            is_demo: true,
            enabled: true,
        };
        assert!(assert_may_sync(&a).is_err());
    }

    #[test]
    fn unified_shows_receiving_account_not_sender() {
        let store = MemoryStore::new();
        let a = AccountId::new("a1").unwrap();
        store
            .upsert_account(&AccountRecord {
                account_id: a.clone(),
                email: "me@work.com".into(),
                display_name: "Me".into(),
                is_demo: false,
                enabled: true,
            })
            .unwrap();
        let msg = MessagePresentation::from_parts(
            a.clone(),
            "me@work.com",
            "m1",
            "t1",
            "sender@other.com",
            "Sender",
            "Hi",
            "",
            10,
        );
        let rows = unified_inbox_join(&store, &[(a, vec![msg])], 10).unwrap();
        assert_eq!(rows.len(), 1);
        assert_eq!(rows[0].receiving.email, "me@work.com");
        assert_ne!(rows[0].receiving.email, rows[0].message.sender_email);
    }

    #[test]
    fn cross_account_message_rejected_in_unified_join() {
        let store = MemoryStore::new();
        let a = AccountId::new("a").unwrap();
        let b = AccountId::new("b").unwrap();
        store
            .upsert_account(&AccountRecord {
                account_id: a.clone(),
                email: "a@ex.com".into(),
                display_name: "A".into(),
                is_demo: false,
                enabled: true,
            })
            .unwrap();
        let foreign = MessagePresentation::from_parts(
            b,
            "a@ex.com",
            "m1",
            "t1",
            "x@y.com",
            "X",
            "leak?",
            "",
            1,
        );
        let err = unified_inbox_join(&store, &[(a, vec![foreign])], 10).unwrap_err();
        assert!(matches!(err, CoreError::AccountIsolationViolation { .. }));
    }
}
