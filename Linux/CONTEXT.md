# Mailstack Linux — Context (Linux only)

> Continuity for **Linux** under `Linux/`. Do not merge into root `CONTEXT.md`.
> Never modify `macOS/`, `Windows/`, `iOS/`, `iPadOS/`, `AndroidTablet/`, or phone `android/` from this workstream.
> Shared Rust core: `desktop-core/`.

Last updated: 2026-10-17

## Standing rules
1. Rust owns business/data logic via `desktop-core`.
2. Linux shell must not duplicate core rules.
3. Demo accounts never sync; no remote mail images.
4. Prefer GTK/iced later; this scaffold validates core on Linux first.

## Skipped / blocked
| Item | Why | Instead |
|------|-----|---------|
| Full GTK/libadwaita GUI | Keep scaffold lean; GUI toolkit not pinned yet | CLI workspace driver over desktop-core |
| Distro packaging (.deb/.rpm/Flatpak) | Later release phases | Checklist |
| Live OAuth / Gmail | No secrets | Demo mailbox |
| Root CONTEXT.md | Platform-local | This file |

## Phase status
| Band | Status |
|------|--------|
| 0 Audit | Done |
| 1–20 Core wiring + mailbox/search/privacy | Source + tests |
| 21–30 Packaging / QA | Checklist |

## Verified
| Check | Result |
|-------|--------|
| `cargo test --manifest-path Linux/Cargo.toml` | PASS + `cargo run` ok |
| `desktop-core` tests | PASS |
