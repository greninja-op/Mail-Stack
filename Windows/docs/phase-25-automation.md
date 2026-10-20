# Phase 25 — Advanced Automation

**Status:** COMPLETE (desktop-core) — 2026-10-20

## Delivered

Shared (`desktop-core/src/automation.rs`):

- Finite triggers (`AutomationTrigger`) and actions (`AutomationActionKind`) — no arbitrary scripts.
- Account-scoped rules; `propose` rejects cross-account execution.
- Engine **proposes**; `requires_confirmation` always true — nothing external automatic.
- Circuit breaker after `CIRCUIT_BREAKER_THRESHOLD` (5) failures.
- `reject_email_authored_automation` — email content cannot create/approve automations.
- `create_rule_from_user` only — user-authored.

Also: `desktop-core/src/capability.rs` integration capability registry (Gmail/Calendar/Tasks/AI/Automation flags).

## Tests

- circuit breaker, email author reject, propose confirmation, cross-account reject.

## Blockers

- Background pipeline integration awaits sync worker wiring; UI builder is interim.
- Agent 5 owns packaging/QA matrices.
