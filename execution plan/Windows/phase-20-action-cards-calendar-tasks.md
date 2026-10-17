# Phase 20 — Action Cards, Calendar & Tasks

## Mission
Implement and verify only Phase 20. Do not implement later phases.

## Mandatory Windows isolation
Modify only the Windows platform boundary and genuinely shared Rust desktop code required by this phase. Never touch macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling areas.

## Required reading
Read root instructions and execution plan/Windows/README.md, requirements.md, spec.md, design.md and editor-rules.md. Inspect the actual repository and current status before editing.

## Objective
Integrate Rust Action Cards with Calendar and Tasks through modular adapters. Provide previews, confirmation levels, account boundaries, deduplication, traceability and failure handling. Email or AI output must never directly authorize external effects.

## Rust-first contract
Rust is the authoritative product/core implementation and must remain reusable by macOS. Keep Windows UI Rust-first. Native Windows SDK calls belong behind narrow Rust adapters. Never move business logic into another language merely for convenience.

## Security and trust
Gmail remains cloud source of truth. Preserve account isolation, least-privilege OAuth, secure credentials, deterministic/local/explainable precedence and untrusted-email rules. Consequential external effects require appropriate confirmation and idempotency.

## Verification
Run targeted and integration tests; build and launch the real Windows application when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart, sleep/wake and recovery states as relevant; verify accessibility, keyboard/pointer behavior, security, data integrity, memory/performance and release configuration where applicable; inspect logs; fix defects; rebuild and retest. Never claim evidence that was not actually observed.

## Documentation
Update status accurately and add only genuinely permanent reusable rules. Record blockers honestly.

**STOP AFTER PHASE 20.**

## 2026-10-18 — Current-design update

- Rust Action Cards with Calendar and Tasks through modular adapters. Previews, confirmation levels, account boundaries, deduplication, traceability, failure handling. Email or AI output must never directly authorize external effects.
- Action Engine: proposes / user confirms / nothing external automatic. Generation and safety validation are separate layers. Deterministic dedup id `messageId|actionType|targetKey`; thread-dedup on (actionType, targetKey). Confirming external proposals honestly reports "not connected" — never fake success.
- Calendar/Tasks use the SAME account + tokens via the one sign-in (no per-integration sign-in). Idempotent creation (Calendar: iCalUID + extended-property key + receipt cache + recovery lookup; Tasks: marker-in-notes + execution store + recovery scan).
- Gmail write (mark read/unread, archive, etc.) behind a confirmation gate: fail-closed, account isolation, idempotency pre-check, Gmail-verified-before-local-update, per-item bulk. 403 → incremental-auth upgrade dialog (gmail.modify is the only incremental scope).
