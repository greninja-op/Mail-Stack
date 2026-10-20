# desktop-core — Context (shared Rust, not a UI platform)

Last updated: 2026-10-18

## Verified this session
| Check | Result |
|-------|--------|
| `cargo test -p mailstack-desktop-core` | **PASS** 18 tests (mailbox, privacy, search, sync, pacing) |

## Notes
- Token-bucket pacing lives here as a **desktop requirement** — do not claim it mirrors Android phone.
- Import travelling-pulse: not present (HELD).

## 2026-10-20
Expanded persistence/oauth/classification/multi-account. `cargo test -p mailstack-desktop-core` → 23 passed.
