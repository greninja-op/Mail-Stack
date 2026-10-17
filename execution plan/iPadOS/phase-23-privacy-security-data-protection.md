# Phase 23 — Privacy, Security & Data Protection

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Perform dedicated iPad security hardening: Keychain/token lifecycle, database protection, account isolation, notification privacy, logging redaction, deep-link validation, HTML sanitization, hostile URLs, attachment handling, pasteboard/screenshot/background-snapshot privacy, URL schemes, entitlements, transport security, dependency audit and secret scanning. Do not make unsupported legal/compliance claims. Test adversarial inputs and cross-account attacks.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Privacy Center: tabbed experience (Overview, Your Data, Google Access, Security) with the full Data Inventory, Google scope disclosures, live security audit checks, and honest local deletion/purge flows that NEVER modify remote Gmail data. Account deletion dialog + local data purge dialog.
- Security hardening: Keychain/token lifecycle, database protection, account isolation, notification privacy, logging redaction (bearer tokens, raw OAuth tokens, authorization headers, email addresses — domains preserved), deep-link validation, HTML sanitization, hostile URLs, attachment handling, pasteboard/screenshot/background-snapshot privacy, URL schemes, entitlements, transport security, dependency audit, secret scanning.
- Safe URL/mailto validators; external link launcher rejects dangerous schemes (javascript:, file:, content:, data:). Email is untrusted input; images never fetched (a fetch tells the sender the mail was opened).
- Backup rules exclude local databases and app-private files from cloud backup / device transfer. Do not make unsupported legal/compliance claims. Test adversarial inputs and cross-account attacks.
