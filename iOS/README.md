# Mailstack — iOS (iPhone)

This directory holds **all iOS / iPhone–specific** planning, source, assets, tests, and configuration for Mailstack.

## Platform isolation (permanent)

- Target: **iOS / iPhone only**.
- Everything iOS-specific lives under this `iOS/` folder.
- Do **not** put iOS files in `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, `Linux/`, or `android/`.
- Do **not** create or modify those sibling platform trees while executing the iOS plan.
- Shared Kotlin Multiplatform (KMP) code, when it exists, stays shared — do not fork it into an iOS-only copy.
- Execution plan documents remain under `execution plan/iOS/` (read-only plan). Implementation artifacts go here.

## Status

See [`docs/development-status.md`](docs/development-status.md).

## Phase 0 (complete)

Architecture audit, folder boundary, and readiness report:

- [`docs/phase-00-architecture-audit.md`](docs/phase-00-architecture-audit.md)
- [`docs/kmp-integration-map.md`](docs/kmp-integration-map.md)
- [`docs/editor-rules.md`](docs/editor-rules.md)

## Next

**Phase 1** — iOS Application Shell & KMP Integration  
Plan: `execution plan/iOS/phase-01-ios-application-shell-kmp-integration.md`

## Honest environment note

This Cloud Agent sandbox is **Linux** and has **no Xcode / iPhone Simulator**. Phase 0 is documentation and boundary only. Simulator/device builds require a macOS host with Xcode (Phase 1+).
