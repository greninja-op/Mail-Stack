# Phase 20 — Action Cards, Calendar & Tasks

**Status:** COMPLETE (2026-10-20)

## Delivered
- `desktop-core/src/actions.rs` — Action Engine propose → confirm; Calendar/Tasks traits
- Dedup id `messageId|actionType|targetKey`; thread-dedup on (actionType, targetKey)
- Disconnected stubs report `NotConnected` — never fake success
- Same account grant for Calendar/Tasks (one sign-in scopes in OAuth module)

## Permanent rules
- Email/AI never directly authorizes external effects.
- Confirm gate is mandatory; disconnected → honest NotConnected.
