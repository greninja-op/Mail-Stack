//! Local persistence contracts: account-scoped store, migrations, cursor save-after-persist.
//!
//! Tokens / passwords are **never** stored in the DB by schema design — credentials
//! live in [`crate::platform::CredentialStore`]. This module ships an in-memory store
//! plus documented SQL migration strings for a later SQLite adapter (no rusqlite link).

use crate::account::{AccountId, AccountScoped};
use crate::error::{CoreError, CoreResult};
use crate::pacing::CursorState;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Mutex;

/// Ordered migrations applied once per fresh database.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Migration {
    pub version: u32,
    pub name: &'static str,
    /// SQL for a future SQLite backend. Documented here; not executed by the
    /// in-memory store (which mirrors the schema semantically).
    pub sql: &'static str,
}

/// Full migration list for Mailstack desktop local DB.
pub fn migrations() -> &'static [Migration] {
    &MIGRATIONS
}

const MIGRATIONS: &[Migration] = &[
    Migration {
        version: 1,
        name: "accounts_and_cursors",
        sql: r#"
CREATE TABLE IF NOT EXISTS accounts (
  account_id TEXT PRIMARY KEY NOT NULL,
  email TEXT NOT NULL,
  display_name TEXT NOT NULL DEFAULT '',
  is_demo INTEGER NOT NULL DEFAULT 0,
  enabled INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS sync_cursors (
  account_id TEXT PRIMARY KEY NOT NULL
    REFERENCES accounts(account_id) ON DELETE CASCADE,
  page_token TEXT,
  history_id TEXT
);
-- Credentials live in CredentialStore only — never as DB columns.
"#,
    },
    Migration {
        version: 2,
        name: "messages_threads",
        sql: r#"
CREATE TABLE IF NOT EXISTS messages (
  account_id TEXT NOT NULL REFERENCES accounts(account_id) ON DELETE CASCADE,
  message_id TEXT NOT NULL,
  thread_id TEXT NOT NULL,
  subject TEXT NOT NULL DEFAULT '',
  snippet TEXT NOT NULL DEFAULT '',
  sender_email TEXT NOT NULL DEFAULT '',
  sender_display_name TEXT NOT NULL DEFAULT '',
  internal_date_ms INTEGER NOT NULL DEFAULT 0,
  is_unread INTEGER NOT NULL DEFAULT 0,
  is_starred INTEGER NOT NULL DEFAULT 0,
  inserted_at_ms INTEGER NOT NULL,
  PRIMARY KEY (account_id, message_id)
);
CREATE INDEX IF NOT EXISTS idx_messages_inserted
  ON messages(account_id, inserted_at_ms DESC);
"#,
    },
    Migration {
        version: 3,
        name: "classification_rules_company",
        sql: r#"
CREATE TABLE IF NOT EXISTS classifications (
  account_id TEXT NOT NULL,
  message_id TEXT NOT NULL,
  category TEXT NOT NULL,
  source TEXT NOT NULL,
  explanation TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (account_id, message_id)
);
CREATE TABLE IF NOT EXISTS user_corrections (
  account_id TEXT NOT NULL,
  message_id TEXT NOT NULL,
  field TEXT NOT NULL,
  value TEXT NOT NULL,
  PRIMARY KEY (account_id, message_id, field)
);
CREATE TABLE IF NOT EXISTS company_pins (
  account_id TEXT NOT NULL,
  company_key TEXT NOT NULL,
  pinned INTEGER NOT NULL DEFAULT 1,
  PRIMARY KEY (account_id, company_key)
);
"#,
    },
    Migration {
        version: 4,
        name: "fts_failure_safe",
        sql: r#"
-- FTS creation is failure-safe: a missing FTS5 module must never abort DB creation.
-- Adapters wrap this in try/catch and continue without FTS when unavailable.
CREATE VIRTUAL TABLE IF NOT EXISTS messages_fts USING fts5(
  account_id UNINDEXED,
  message_id UNINDEXED,
  subject,
  snippet,
  sender_email,
  sender_display_name,
  tokenize = 'unicode61'
);
"#,
    },
];

/// Opaque persisted message row (account-scoped).
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct StoredMessage {
    pub account_id: AccountId,
    pub message_id: String,
    pub thread_id: String,
    pub subject: String,
    pub snippet: String,
    pub sender_email: String,
    pub sender_display_name: String,
    pub internal_date_ms: i64,
    pub is_unread: bool,
    pub is_starred: bool,
    pub inserted_at_ms: i64,
}

impl AccountScoped for StoredMessage {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

/// Account metadata row. Demo accounts never sync (enforced by sync engine).
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct AccountRecord {
    pub account_id: AccountId,
    pub email: String,
    pub display_name: String,
    pub is_demo: bool,
    pub enabled: bool,
}

impl AccountScoped for AccountRecord {
    fn account_id(&self) -> &AccountId {
        &self.account_id
    }
}

/// Trait for local persistence. SQLite adapter will implement the same surface.
pub trait LocalStore: Send + Sync {
    fn upsert_account(&self, account: &AccountRecord) -> CoreResult<()>;
    fn get_account(&self, account_id: &AccountId) -> CoreResult<Option<AccountRecord>>;
    fn list_accounts(&self) -> CoreResult<Vec<AccountRecord>>;

    /// Persist a full page of messages, **then** save the cursor.
    /// Cursor must not advance if persist fails.
    fn persist_page_then_cursor(
        &self,
        account_id: &AccountId,
        messages: &[StoredMessage],
        cursor: &CursorState,
    ) -> CoreResult<()>;

    fn load_cursor(&self, account_id: &AccountId) -> CoreResult<CursorState>;
    fn recent_inserted(&self, account_id: &AccountId, limit: usize) -> CoreResult<Vec<StoredMessage>>;
    fn applied_migration_version(&self) -> CoreResult<u32>;
}

#[derive(Default)]
struct MemoryInner {
    accounts: HashMap<String, AccountRecord>,
    messages: HashMap<(String, String), StoredMessage>,
    cursors: HashMap<String, CursorState>,
    migration_version: u32,
}

/// In-memory store for unit tests and early Windows bring-up (no native SQLite link).
pub struct MemoryStore {
    inner: Mutex<MemoryInner>,
}

impl MemoryStore {
    pub fn new() -> Self {
        let mut inner = MemoryInner::default();
        // Apply all migrations conceptually (SQL documented; in-memory is always "current").
        inner.migration_version = migrations().last().map(|m| m.version).unwrap_or(0);
        Self {
            inner: Mutex::new(inner),
        }
    }

    fn lock(&self) -> CoreResult<std::sync::MutexGuard<'_, MemoryInner>> {
        self.inner
            .lock()
            .map_err(|_| CoreError::Internal("persistence lock poisoned".into()))
    }
}

impl Default for MemoryStore {
    fn default() -> Self {
        Self::new()
    }
}

impl LocalStore for MemoryStore {
    fn upsert_account(&self, account: &AccountRecord) -> CoreResult<()> {
        let mut g = self.lock()?;
        g.accounts
            .insert(account.account_id.as_str().to_owned(), account.clone());
        Ok(())
    }

    fn get_account(&self, account_id: &AccountId) -> CoreResult<Option<AccountRecord>> {
        let g = self.lock()?;
        Ok(g.accounts.get(account_id.as_str()).cloned())
    }

    fn list_accounts(&self) -> CoreResult<Vec<AccountRecord>> {
        let g = self.lock()?;
        Ok(g.accounts.values().cloned().collect())
    }

    fn persist_page_then_cursor(
        &self,
        account_id: &AccountId,
        messages: &[StoredMessage],
        cursor: &CursorState,
    ) -> CoreResult<()> {
        let mut g = self.lock()?;
        for m in messages {
            crate::account::require_account(account_id, &m.account_id)?;
            g.messages
                .insert((account_id.as_str().to_owned(), m.message_id.clone()), m.clone());
        }
        // Cursor saved only after page fully persists.
        g.cursors
            .insert(account_id.as_str().to_owned(), cursor.clone());
        Ok(())
    }

    fn load_cursor(&self, account_id: &AccountId) -> CoreResult<CursorState> {
        let g = self.lock()?;
        Ok(g.cursors
            .get(account_id.as_str())
            .cloned()
            .unwrap_or_else(CursorState::empty))
    }

    fn recent_inserted(&self, account_id: &AccountId, limit: usize) -> CoreResult<Vec<StoredMessage>> {
        let g = self.lock()?;
        let mut rows: Vec<_> = g
            .messages
            .values()
            .filter(|m| m.account_id == *account_id)
            .cloned()
            .collect();
        rows.sort_by(|a, b| b.inserted_at_ms.cmp(&a.inserted_at_ms));
        rows.truncate(limit);
        Ok(rows)
    }

    fn applied_migration_version(&self) -> CoreResult<u32> {
        Ok(self.lock()?.migration_version)
    }
}

/// Schema audit: ensure documented SQL never introduces credential columns.
/// SQL line comments (`-- …`) are stripped so documentation text cannot false-positive.
pub fn schema_forbids_tokens(sql: &str) -> bool {
    let stripped: String = sql
        .lines()
        .map(|line| match line.find("--") {
            Some(i) => &line[..i],
            None => line,
        })
        .collect::<Vec<_>>()
        .join("\n")
        .to_ascii_lowercase();
    !stripped.contains("refresh_token")
        && !stripped.contains("access_token")
        && !stripped.contains("password")
        && !stripped.contains("client_secret")
}

#[cfg(test)]
mod tests {
    use super::*;

    fn acct(id: &str) -> AccountId {
        AccountId::new(id).unwrap()
    }

    #[test]
    fn migrations_are_ordered_and_token_free() {
        let m = migrations();
        assert!(!m.is_empty());
        for (i, mig) in m.iter().enumerate() {
            assert_eq!(mig.version as usize, i + 1);
            assert!(schema_forbids_tokens(mig.sql), "migration {} leaked secrets", mig.name);
        }
    }

    #[test]
    fn cursor_saved_only_after_persist() {
        let store = MemoryStore::new();
        let a = acct("a1");
        store
            .upsert_account(&AccountRecord {
                account_id: a.clone(),
                email: "a@ex.com".into(),
                display_name: "A".into(),
                is_demo: false,
                enabled: true,
            })
            .unwrap();

        let msg = StoredMessage {
            account_id: a.clone(),
            message_id: "m1".into(),
            thread_id: "t1".into(),
            subject: "Hi".into(),
            snippet: "body".into(),
            sender_email: "s@co.com".into(),
            sender_display_name: "S".into(),
            internal_date_ms: 1,
            is_unread: true,
            is_starred: false,
            inserted_at_ms: 100,
        };
        let cursor = CursorState {
            page_token: Some("page-2".into()),
            history_id: Some("h1".into()),
        };
        store
            .persist_page_then_cursor(&a, &[msg], &cursor)
            .unwrap();
        let loaded = store.load_cursor(&a).unwrap();
        assert_eq!(loaded.page_token.as_deref(), Some("page-2"));
        assert_eq!(store.recent_inserted(&a, 2).unwrap().len(), 1);
        assert_eq!(store.applied_migration_version().unwrap(), 4);
    }

    #[test]
    fn account_isolation_on_persist() {
        let store = MemoryStore::new();
        let a = acct("a1");
        let b = acct("b1");
        let msg = StoredMessage {
            account_id: b,
            message_id: "m1".into(),
            thread_id: "t1".into(),
            subject: String::new(),
            snippet: String::new(),
            sender_email: String::new(),
            sender_display_name: String::new(),
            internal_date_ms: 0,
            is_unread: false,
            is_starred: false,
            inserted_at_ms: 1,
        };
        let err = store
            .persist_page_then_cursor(&a, &[msg], &CursorState::empty())
            .unwrap_err();
        assert!(matches!(err, CoreError::AccountIsolationViolation { .. }));
        // Cursor must not advance on failure.
        assert_eq!(store.load_cursor(&a).unwrap(), CursorState::empty());
    }
}
