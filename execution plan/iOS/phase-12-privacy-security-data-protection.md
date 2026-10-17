# Phase 12 — iOS Privacy, Security & Data Protection

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. All iOS-specific security/config/docs/tests remain under `iOS/`. Do not touch sibling platforms.

## Mission

Harden the iOS application and make privacy/data handling explicit and verifiable.

## Scope

- Keychain storage review
- token lifecycle
- data minimization
- account isolation
- local database protection
- sensitive notification handling
- logging/redaction
- URL/deep-link validation
- email HTML sanitization
- attachment handling
- pasteboard/privacy-sensitive surfaces
- screenshots/background snapshot privacy where appropriate
- exported URL schemes/universal links
- entitlements audit
- network transport security
- dependency/security audit
- privacy center hooks
- account disconnect/data cleanup

Do not make unsupported legal/compliance claims.

## Validation

Perform static inspection, dependency audit, secret scan, log review, entitlement review, network/config review, malicious HTML tests, hostile URLs, malformed data, and cross-account attack tests.

**STOP AFTER PHASE 12.**

## 2026-10-18 — Current-design update

- Privacy Center: tabbed experience (Overview, Your Data, Google Access, Security) with the full Data Inventory, Google scope disclosures, live security audit checks, and honest local deletion/purge flows that NEVER modify remote Gmail data. Account deletion dialog + local data purge dialog.
- Logging sanitizer: redact bearer tokens, raw OAuth tokens, authorization headers, email addresses (preserve domains). Safe URL/mailto validators; external link launcher rejects dangerous schemes (javascript:, file:, content:, data:).
- Backup rules exclude local databases and app-private files from cloud backup / device transfer. Tokens/secrets never in DB, logs, UI, or screenshots — Keychain only.
- Email is untrusted input: HTML sanitized; images never fetched (a fetch tells the sender the mail was opened).
