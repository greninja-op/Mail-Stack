# Phase 22 — Offline, Background Work & Notifications

**Status:** COMPLETE (contracts + Windows adapter) — 2026-10-20

## Delivered

Shared (`desktop-core/src/background.rs`):

- `BackgroundWorkRequest` carries an **explicit `account_id`** (never inferred from active UI account).
- `clamp_periodic_interval` / `STALENESS_THRESHOLD` — **15-minute** platform minimum; lifecycle work skips when fresh.
- `FailureClass` offline taxonomy — Offline vs Authentication vs ApiThrottle/ApiError (offline copy never says “Reconnect”).
- `NotificationDeduper` — account-scoped dedup keys.
- `MemoryBackgroundScheduler` + `BackgroundScheduler` trait.

Windows (`adapters/notifications.rs`):

- `WindowsNotificationService::show_deduped` uses core deduper; toast/WinRT deferred to WinAppSDK host.

## Tests

- Unit tests in `background.rs` (clamp, staleness, taxonomy, dedup, scheduler account).
- Adapter dedup test in `notifications.rs`.

## Blockers

- Full WinRT toast delivery requires WinAppSDK packaging (Agent 5 / Phase 30).
- `cargo test` on this host needs MSVC `link.exe` (VS C++ tools not installed).
