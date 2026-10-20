# Phase 12 — Incremental Sync, Pagination & Recovery

**Status:** COMPLETE (2026-10-20)  
**Boundary:** `desktop-core/src/sync_engine.rs`.

## Delivered

| Item | Location |
|------|----------|
| Incremental sync resumes from saved cursor | `run_incremental_sync` |
| Join in-flight per account (no cancel/restart storm) | `SyncCoordinator::try_begin` / `is_in_flight` |
| Cursor only after full page persist | `persist_page_then_cursor` |
| Honest failure copy: throttle ≠ daily ≠ offline ≠ Reconnect | `sync_error_user_message`, `CoreError::DailyQuota` |

## Verification

- `incremental_resumes_from_cursor`, `join_inflight_rejects_second`, `offline_message_is_not_reconnect`, `daily_quota_message_distinct`.
- Full crate: **`cargo test -p desktop-core` → 95 passed; 0 failed** (2026-10-20, MSVC `link.exe` via VS 2017 Build Tools on PATH).

## Blockers

- History API (`users.history.list`) gap/stale-cursor recovery not yet a separate code path — page-token resume only.
- Concurrent callers currently get `InvalidArgument` rather than a progress-subscription join handle (UI observes existing progress).
