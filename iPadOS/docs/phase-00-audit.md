# iPadOS Phase 0 — Audit

## Inventory
- Phone Android: `android/` (reference UI)
- iPhone: `iOS/` (SwiftUI shell)
- Shared KMP: `shared/` (JVM contracts)
- iPad target: **this folder** (new)

## Layout states (content-driven)
compactSingle | regularSplit | expandedTriple | inspectorOpen — never device-name checks.

## Blockers
No Xcode on Linux agent; Simulator validation skipped (see CONTEXT.md).
