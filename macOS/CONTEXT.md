# Mailstack macOS — Context (macOS only)

> Continuity for **macOS** under `macOS/`. Do not merge into root `CONTEXT.md`.
> Never modify `iOS/`, `iPadOS/`, `AndroidTablet/`, `Windows/`, or `Linux/` from this workstream.
> Shared Rust core: `desktop-core/` (platform-neutral only).

Last updated: 2026-10-17

## Standing rules
1. Rust (`desktop-core/`) owns business/data logic; SwiftUI is presentation + OS integration.
2. Do not duplicate business rules in Swift.
3. Desktop UX: NavigationSplitView, menus, keyboard shortcuts — not a stretched phone UI.
4. Demo accounts (`local-*`, `fixture-*`) never sync.
5. Linux agent: no Xcode — Simulator/signing skipped.

## Skipped / blocked
| Item | Why | Instead |
|------|-----|---------|
| Xcode / macOS run | Linux host | Swift sources + XcodeGen |
| UniFFI / cxx bridge to desktop-core | Needs Mac packaging | Swift demo store mirrors core contracts |
| Live OAuth / Gmail | No secrets | Demo |
| Notarization / Developer ID | No Apple creds | Checklist |
| Root CONTEXT.md | Platform-local only | This file |

## Phase status
| Band | Status |
|------|--------|
| 0 Audit | Done (docs) |
| 1–15 Foundation → mailbox/search | Source scaffold |
| 16–30 Hardening / QA / release | Checklists; device skipped |

## Verified
| Check | Result |
|-------|--------|
| `desktop-core` `cargo test` | PASS (4 tests) |
| macOS Swift compile | SKIPPED |
