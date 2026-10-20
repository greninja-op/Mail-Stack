# Phase 23 — Privacy, Security & Data Protection

**Status:** COMPLETE (model + launcher + UI tabs) — 2026-10-20

## Delivered

Shared (`desktop-core/src/privacy.rs`):

- `PrivacyCenterModel` / `PrivacyCenterTab` — Overview, Your Data, Google Access, Security.
- `sanitize_log_line` — bearer/auth/token redaction; emails keep domain only.
- `validate_external_url` / `launch_safe_link` — **blocks `javascript:`, `file:`, `content:`, `data:`, `vbscript:`**.
- `LocalPurgePlan` — local-only; `modifies_remote_gmail` always false; validate rejects remote mutation.
- Data inventory entries all declare `remote_gmail_modified_on_purge: false`.

Windows:

- Tabbed Privacy Center UI (`ui/privacy_center.rs`).
- Browser adapter routes through shared URL safety (`adapters/browser.rs` + `open_safe_link`).
- Existing `redacting_log.rs` remains for emit-time sanitization.

## Tests

- privacy.rs: redact, scheme reject, safe launcher, purge contract, four tabs.

## Blockers

- Persistence-backed wipe execution waits on Phase 07 store wiring maturity.
- Threat-model sign-off owned by Phase 29 (Agent 5).
