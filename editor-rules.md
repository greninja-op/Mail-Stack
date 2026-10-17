# Mailstack — Root Editor Rules

## Cross-platform isolation (permanent — established iOS Phase 0, 2026-10-18)

Each platform plan owns its own root folder. Never mix them:

| Plan | Implementation root | Plan docs |
|------|---------------------|-----------|
| Android (phone) | `android/` | `execution plan/android/` |
| iOS (iPhone) | `iOS/` | `execution plan/iOS/` |
| iPadOS | `iPadOS/` | `execution plan/iPadOS/` |
| Android tablet | `AndroidTablet/` | `execution plan/AndroidTablet/` |
| macOS | `macOS/` | `execution plan/macOS/` |
| Windows | `Windows/` | `execution plan/Windows/` |
| Linux | `Linux/` | `execution plan/Linux/` |

Rules:

1. When executing a platform plan, create/modify files **only** under that platform’s implementation root (plus shared KMP when the phase explicitly requires it).
2. Do not create sibling platform roots “for convenience.”
3. Do not copy shared business logic into a platform folder to satisfy isolation.
4. Continuity and product rules live in root `CONTEXT.md` — read before any work; update after every prompt.

## Continuity

Always follow root `CONTEXT.md` standing rules (R1–R64) and the active platform’s `execution plan/<platform>/editor-rules.md`.


## Platform-local CONTEXT.md

Each platform root maintains its own `CONTEXT.md` for continuity and **skipped** items
(`iOS/CONTEXT.md`, `iPadOS/CONTEXT.md`, `AndroidTablet/CONTEXT.md`, `macOS/CONTEXT.md`,
`Windows/CONTEXT.md`, `Linux/CONTEXT.md`). Prefer updating those during platform work
instead of mixing platform skip logs into unrelated roots.
