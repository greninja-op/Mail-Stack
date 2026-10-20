# Phase 06 — Rust Core Integration & Dependency Boundary

**Status:** COMPLETE (boundary) — 2026-10-20

## Delivered

- `bridge.rs` — UI façade over `desktop-core` (sanitize, sync snapshot, category groups, cancel)
- `redacting_log.rs` — redacts bearer/OAuth tokens, Authorization headers, email local-parts (keeps domain)
- `adapters/` — Credential Manager attempt, browser open, notification stub

## Logging sanitizer tests

- Email → `[REDACTED]@domain`
- `Bearer …` / `access_token=` → `[REDACTED]`
