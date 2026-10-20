# Phase 19 — Temporal & Conversation Intelligence

**Status:** COMPLETE (2026-10-20)

## Delivered
- `desktop-core/src/temporal.rs` — 14 temporal item types; reference time = message received; idempotent extract
- `desktop-core/src/conversation.rs` — 7 states; negative-signal shield; 3-day follow-up / 7-day stale
- Conversation viewer surfaces state label + temporal cues

## Permanent rules
- Never equate “no reply” with “user forgot”.
- Extracted facts ≠ confirmed Calendar/Tasks objects.
