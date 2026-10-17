# Mailstack Windows — Context (Windows only)

> Continuity for **Windows** under `Windows/`. Do not merge into root `CONTEXT.md`.
> Never modify `macOS/`, `Linux/`, `iOS/`, `iPadOS/`, `AndroidTablet/`, or phone `android/` from this workstream.
> Shared Rust core: `desktop-core/`.

Last updated: 2026-10-17

## Standing rules
1. Rust owns business/data logic via `desktop-core`.
2. Windows shell must not duplicate core rules.
3. Demo accounts never sync; no remote mail images.
4. This agent runs on Linux — native Win32 GUI / MSVC packaging skipped.

## Skipped / blocked
| Item | Why | Instead |
|------|-----|---------|
| WinUI / native Windows GUI on device | Linux host | CLI workspace driver over desktop-core |
| MSVC / MSI / Authenticode | No Windows SDK here | Checklist |
| Live OAuth / Gmail | No secrets | Demo mailbox from core |
| Root CONTEXT.md | Platform-local | This file |

## Phase status
| Band | Status |
|------|--------|
| 0 Audit | Done |
| 1–20 Core wiring + mailbox/search/privacy | Source + tests via core |
| 21–30 Multi-account / QA / installer | Partial / checklist |

## Verified
| Check | Result |
|-------|--------|
| `cargo test -p mailstack-windows` (Linux cross-check) | PASS |
| `desktop-core` tests | PASS |
