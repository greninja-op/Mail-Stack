# Mailstack iOS — Permanent Editor Rules

Master plan rules: `execution plan/iOS/editor-rules.md` and `execution plan/iOS/design.md` (2026-10-18 current design).  
Repo continuity: root `CONTEXT.md` (R1–R64).

## Platform isolation (Phase 0 — permanent)

1. This work targets **iOS / iPhone only**.
2. All iOS-specific documents, source, assets, tests, scripts, entitlements, signing references, and release config live under repository-root **`iOS/`**.
3. Never create or modify `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/` during the iOS plan.
4. Never place iOS-specific code under `android/`.
5. Shared KMP remains shared. Do not duplicate shared business logic into Swift or into `iOS/` merely for folder separation.
6. Shared-module changes are allowed only when the current iOS phase explicitly requires them; document why.
7. Never touch OmniBuds or unrelated projects.

## Phase protocol

- Execute **one phase at a time**. Inspect → implement → verify honestly → update status → **stop**.
- Do not start the next phase automatically.
- Never claim simulator, device, OAuth, or sync verification that was not actually performed.
- Never commit secrets.

## Design authority (2026-10-18)

Where phase docs disagree with `execution plan/iOS/design.md` / `editor-rules.md` (2026-10-18 update), those design files win. Match the current Android Mailstack UI philosophy (1.0.39 + in-main), adapted to SwiftUI / HIG — do not re-derive from outdated plan prose.
