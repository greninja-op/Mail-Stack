# Phase 23 — Privacy, Security & Data Protection

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Audit Android secure storage, tokens, database protection, account isolation, notifications, logs/redaction, deep links, HTML sanitization, hostile URLs, attachments, clipboard/screenshot/background-preview privacy, manifest permissions, transport security and dependency integrity. Run secret scan and adversarial tests. Do not make unsupported compliance claims.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 23.**

## 2026-10-18 — Current-design update

- Privacy Center: tabbed experience (Overview, Your Data, Google Access, Security) with the full Data Inventory, Google scope disclosures, live security audit checks, and honest local deletion/purge flows that NEVER modify remote Gmail data. Account deletion dialog + local data purge dialog.
- Logging sanitizer: redact bearer tokens, raw OAuth tokens, authorization headers, email addresses (preserve domains). Safe URL/mailto validators; external link launcher rejects dangerous schemes (javascript:, file:, content:, data:).
- Backup rules exclude local databases and app-private files from cloud backup / device transfer. Tokens/secrets never in DB, logs, UI, or screenshots.
- Email is untrusted input: HTML sanitized; images never fetched (a fetch tells the sender the mail was opened). Audit clipboard/screenshot/background-preview privacy, manifest permissions, transport security, and dependency integrity. Run secret scan and adversarial tests. Do not make unsupported compliance claims.
