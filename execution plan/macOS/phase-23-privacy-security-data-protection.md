## Mandatory Rust desktop architecture

**Rust is the primary implementation language for this macOS phase.** Platform-neutral business/data/application logic belongs in the reusable Rust desktop core, which must also serve Windows. Swift/SwiftUI is for native macOS UI and OS integration only and must not duplicate Rust business rules.

# Phase 23 — Privacy, Security & Data Protection

## Mission
Implement and verify only Phase 23. Do not implement later phases.

## macOS isolation
Keep all macOS-specific source, UI, tests, assets, entitlements and configuration in the macOS boundary. Never modify sibling platforms. Shared Rust desktop core changes must be genuinely platform-neutral and required by this phase.

## Required reading
Read root instructions and all macOS master documents. Inspect the actual repository, current status and existing architecture before editing.

## Scope
Perform the dedicated privacy/security implementation pass: data minimization, Keychain boundaries, OAuth scopes, local storage, HTML and URL safety, logs, crash data, notifications, analytics, AI data boundaries, disconnect cleanup and threat-model fixes.

## Safety and architecture
Preserve Gmail as cloud source of truth and explicit account scoping. Keep business logic shared where platform-neutral. External actions require appropriate authorization, confirmation and idempotency. Treat email, URLs, attachments and external model output as untrusted.

## Verification
Run targeted unit/integration tests; build and launch the actual macOS app when possible; test realistic success and failure states including offline, cancellation, restart, recovery and partial failure as applicable; verify accessibility, security, data integrity and performance; inspect logs for unexpected errors or secrets; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status accurately and add only permanent rules.

**STOP AFTER PHASE 23.**

## 2026-10-18 — Current-design update

- Privacy Center: tabbed experience (Overview, Your Data, Google Access, Security) with the full Data Inventory, Google scope disclosures, live security audit checks, and honest local deletion/purge flows that NEVER modify remote Gmail data. Account deletion dialog + local data purge dialog.
- Logging sanitizer: redact bearer tokens, raw OAuth tokens, authorization headers, email addresses (preserve domains). Safe URL/mailto validators; external link launcher rejects dangerous schemes (javascript:, file:, content:, data:).
- Backup/exfiltration rules exclude local databases and app-private files from cloud backup / device transfer (Time Machine / Migration Assistant awareness). Tokens/secrets never in DB, logs, UI, or screenshots — Keychain only.
- Email is untrusted input: HTML sanitized; images never fetched (a fetch tells the sender the mail was opened). AI data boundaries enforced (see Phase 24).
