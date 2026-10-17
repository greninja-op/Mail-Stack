## Mandatory Rust desktop architecture

**Rust is the primary implementation language for this macOS phase.** Platform-neutral business/data/application logic belongs in the reusable Rust desktop core, which must also serve Windows. Swift/SwiftUI is for native macOS UI and OS integration only and must not duplicate Rust business rules.

# Phase 22 — Offline, Background Refresh & Notifications

## Mission
Implement and verify only Phase 22. Do not implement later phases.

## macOS isolation
Keep all macOS-specific source, UI, tests, assets, entitlements and configuration in the macOS boundary. Never modify sibling platforms. Shared Rust desktop core changes must be genuinely platform-neutral and required by this phase.

## Required reading
Read root instructions and all macOS master documents. Inspect the actual repository, current status and existing architecture before editing.

## Scope
Implement offline behavior, macOS-appropriate refresh/background work and notifications. Handle network loss, sleep/wake, termination, relaunch, stale work, cancellation, duplicate notifications, notification authorization/privacy and power constraints.

## Safety and architecture
Preserve Gmail as cloud source of truth and explicit account scoping. Keep business logic shared where platform-neutral. External actions require appropriate authorization, confirmation and idempotency. Treat email, URLs, attachments and external model output as untrusted.

## Verification
Run targeted unit/integration tests; build and launch the actual macOS app when possible; test realistic success and failure states including offline, cancellation, restart, recovery and partial failure as applicable; verify accessibility, security, data integrity and performance; inspect logs for unexpected errors or secrets; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status accurately and add only permanent rules.

**STOP AFTER PHASE 22.**

## 2026-10-18 — Current-design update

- Background work carries an explicit account id and never reads the active UI account (protects against races when switching accounts). Periodic intervals clamped to the platform minimum; lifecycle triggers apply a 15-minute staleness threshold (no aggressive polling).
- Offline state is cleanly distinguished from authentication failure or API errors — never show "Reconnect" when simply offline. Last-sync timestamps are truthful, no fake precision.
- macOS: respect App Nap / sleep-wake / termination / relaunch constraints. Never claim background work runs continuously or at an exact cadence macOS does not guarantee. Notifications are honest (no fake/bait content); duplicate notifications suppressed; authorization/privacy respected.
