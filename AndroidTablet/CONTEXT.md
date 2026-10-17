# Mailstack Android Tablet — Context (tablet only)

> Continuity for **Android tablet** under `AndroidTablet/`. Do not merge into root `CONTEXT.md`.
> Never modify `android/` (phone), `iOS/`, `iPadOS/`, `macOS/`, `Windows/`, or `Linux/` from this workstream.

Last updated: 2026-10-17

## Standing rules
1. All tablet artifacts live under `AndroidTablet/`.
2. Prefer consuming shared KMP (`shared/`) over copying phone UI code.
3. Large-screen: list-detail layouts; content-driven breakpoints, not device-name checks.
4. Same contracts: fixed sync denominator, no remote mail images, demo accounts never sync.
5. Compile/test with Gradle when SDK is available; record skips honestly.

## Skipped / blocked
| Item | Why | Instead |
|------|-----|---------|
| Emulator GUI verification | No interactive emulator session | Unit tests + compile |
| Live Google OAuth / Gmail | No client secrets | Demo mailbox |
| Play signing | No tablet keystore | Checklist |
| Modifying phone `android/` | Isolation | Standalone module |
| Root CONTEXT.md | Platform-local only | This file |
| material3-adaptive APIs may need BOM tweaks | Dependency versions | Simpler Row+list/detail fallback if compile fails |

## Phase status
| Band | Status |
|------|--------|
| 0 Audit | Done |
| 1–15 Shell → a11y | Source scaffold |
| 16–30 QA / release | Unit test + checklists; device QA skipped |

## Decisions
| Date | Decision |
|------|----------|
| 2026-10-17 | Standalone Gradle project under `AndroidTablet/` (phone `android/` untouched). |
| 2026-10-17 | Demo account `local-tablet-demo`. |

## Verified on this agent (2026-10-17)
| Check | Result |
|-------|--------|
| `./gradlew :app:testDebugUnitTest` | PASS |
| Phone `android/` modified | No |
| Emulator GUI | SKIPPED |
