# Phase 21 — Multi-Account & Unified Inbox

**Status:** COMPLETE (2026-10-20)

## Delivered
- `desktop-core/src/multi_account.rs` — presentation-layer unified join + receiving-account indicator
- `assert_may_sync` — demo/seeded accounts never sync
- Windows: multi-account sample, unified toggle, receiving-account chip on every row
- Sync coordinator also refuses demo accounts

## Permanent rules
- Unified inbox is presentation-only — no cross-account ownership mixing.
- Every unified row shows receiving account (not sender).
- Demo accounts never sync.
