# Phase 11 — Initial Gmail Sync

**Status:** COMPLETE (2026-10-20)  
**Boundary:** `desktop-core/src/sync_engine.rs` (`ImportFlowState`, `SyncCoordinator::run_initial_sync`).

## Delivered

| Item | Location |
|------|----------|
| Profile `messagesTotal` fetched once → fixed progress denominator | `run_initial_sync` |
| Progress caps 99% until done → 100% | `SyncProgress` |
| List → never-skip get → persist page → then cursor | same + `LocalStore` |
| Import-flow model: continue gated on Done; recent 2 inserted ids | `ImportFlowState` |
| Demo accounts never sync | `refuse_demo` |

## Verification

- `initial_sync_progress_done_enables_continue`, `demo_accounts_never_sync`.

## Blockers

- Import-flow **UI** (arc pulse, logos, flying mails) owned by Windows shell agent — core supplies state only.
- Live Gmail first-sync not run (mock only).
