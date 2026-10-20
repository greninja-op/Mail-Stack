# Mailstack — Root Editor Rules

## Cross-platform isolation (permanent)

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

1. When executing a platform plan, create/modify files **only** under that platform's implementation root (plus shared KMP / `desktop-core` when the phase explicitly requires it).
2. Do not create sibling platform roots for convenience.
3. Do not copy shared business logic into a platform folder to satisfy isolation.
4. Continuity lives in root `CONTEXT.md` — read before any work; update after every prompt.
5. Follow `execution plan/<platform>/` (`design.md`, `requirements.md`, `editor-rules.md`, phase files). Do not invent alternate product shapes (for example CLI-only shells) when the plan requires a real platform UI.
6. Never modify `android/` unless the user explicitly asks. Android may be read as UI reference only.

## Continuity

Always follow root `CONTEXT.md` standing rules and the active platform's `execution plan/<platform>/editor-rules.md`.
