# Mailstack iPadOS — Context (iPadOS only)

> Continuity for **iPad** under `iPadOS/`. Do not merge into root `CONTEXT.md`.
> Never modify `iOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/` from this workstream.

Last updated: 2026-10-17

## Standing rules
1. All iPadOS artifacts live under `iPadOS/`.
2. Shared KMP stays in `shared/` — do not duplicate into this folder.
3. Multi-column workspace via `NavigationSplitView` (content-driven widths, never device-name checks).
4. Same product contracts as phone: fixed sync denominator, no remote mail images, Keychain tokens, demo accounts never sync.
5. Linux agent: no Xcode / iPad Simulator — skips listed below.

## Skipped / blocked
| Item | Phase | Why | Instead |
|------|-------|-----|---------|
| iPad Simulator build/launch | 0+ | Linux host | XcodeGen project + Swift sources |
| Stage Manager / Split View on device | 0 / 5 | No Simulator | Layout states coded via column visibility |
| Live OAuth / Gmail | 3–4 | No secrets / Mac | Demo ports |
| XCFramework KMP link | 1 | Needs Mac | Ports mirror `shared/` |
| Signing / App Store | late | No Apple creds | Checklist docs |
| Root CONTEXT.md updates | — | Platform-local CONTEXT only | This file |

## Phase status
| Phase band | Status | Notes |
|------------|--------|-------|
| 0 Audit | Done | docs/phase-00-audit.md |
| 1–5 Shell, persistence, oauth seams, sync, multi-column mailbox | Source done | Simulator skipped |
| 6–10 Intelligence, search, rules, actions, multi-account | Source done | |
| 11–15 Offline, privacy, AI, automation, a11y | Source done | |
| 16–30 QA / signing / release | Checklist / tests authored | Device QA skipped |

## Decisions
| Date | Decision |
|------|----------|
| 2026-10-17 | Create platform-local CONTEXT.md; do not edit root CONTEXT.md for platform work. |
| 2026-10-17 | iPad app uses NavigationSplitView + shared design tokens; demo account `local-ipad-demo`. |
