# Phase 07 — Local Persistence & State Hydration

**Status:** COMPLETE (2026-10-20)  
**Boundary:** `desktop-core` (platform-neutral). No Android / other-platform edits.

## Delivered

| Item | Location |
|------|----------|
| Migration list (accounts, cursors, messages, classification/rules/company, FTS failure-safe) | `desktop-core/src/persistence.rs` |
| `LocalStore` + `MemoryStore` (account-scoped) | same |
| Cursor saved **only after** page persist | `persist_page_then_cursor` |
| No token/password columns (schema audit) | `schema_forbids_tokens` |
| Demo accounts marked in `AccountRecord.is_demo` | sync engine refuses them (Phase 11) |

## Verification

- `cargo test -p desktop-core` — persistence tests: migrations ordered + token-free, cursor-after-persist, account isolation.
- In-memory store only this phase; SQLite adapter deferred (SQL documented in migrations).

## Blockers

- Native SQLite (`rusqlite`) not linked yet — MemoryStore is the bring-up backend.
- Fresh-DB / restart hydration against a real file DB not exercised until SQLite lands.
