# Phase 10 — Gmail Data Access & API Adapter

**Status:** COMPLETE (2026-10-20)  
**Boundary:** `desktop-core/src/gmail.rs` (+ pacing from Phase 02).

## Delivered

| Item | Location |
|------|----------|
| `GmailApi` trait (profile, list, get) | `gmail.rs` |
| Provider error taxonomy (throttle / daily / scopes / auth / offline) | `GmailApiErrorKind`, `classify_provider_error` |
| `get_message_never_skip` — retries throttled GETs, never drops | same + `RequestPacer` |
| Token-bucket ~10 req/s + `Retry-After` honor | `TokenBucketPacer` |
| `MockGmail` for sync tests | same |

## Verification

- `classify_throttle`, `never_skip_retries_throttled_get` pass under `cargo test -p desktop-core`.

## Blockers

- Live HTTP Gmail client (reqwest + OAuth bearer) not wired — mock/contracts only.
- Do not fire retry LIST while throttling (documented; mock list is single-page).
