# Phase 22 — Offline, Background Work & Notifications

## Mission
Implement and verify only Phase 22. Do not implement later phases.

## Mandatory Windows isolation
Modify only the Windows platform boundary and genuinely shared Rust desktop code required by this phase. Never touch macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling areas.

## Required reading
Read root instructions and execution plan/Windows/README.md, requirements.md, spec.md, design.md and editor-rules.md. Inspect the actual repository and current status before editing.

## Objective
Implement Windows-appropriate offline behavior, background work and notifications through Rust adapters. Handle network loss, sleep/wake, termination, relaunch, stale work, cancellation, duplicate notifications, notification privacy/authorization and power constraints.

## Rust-first contract
Rust is the authoritative product/core implementation and must remain reusable by macOS. Keep Windows UI Rust-first. Native Windows SDK calls belong behind narrow Rust adapters. Never move business logic into another language merely for convenience.

## Security and trust
Gmail remains cloud source of truth. Preserve account isolation, least-privilege OAuth, secure credentials, deterministic/local/explainable precedence and untrusted-email rules. Consequential external effects require appropriate confirmation and idempotency.

## Verification
Run targeted and integration tests; build and launch the real Windows application when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart, sleep/wake and recovery states as relevant; verify accessibility, keyboard/pointer behavior, security, data integrity, memory/performance and release configuration where applicable; inspect logs; fix defects; rebuild and retest. Never claim evidence that was not actually observed.

## Documentation
Update status accurately and add only genuinely permanent reusable rules. Record blockers honestly.

**STOP AFTER PHASE 22.**

## 2026-10-18 — Current-design update

- Windows-appropriate offline behavior, background work, notifications through Rust adapters. Network loss, sleep/wake, termination, relaunch, stale work, cancellation, duplicate notifications, notification privacy/authorization, power constraints.
- Background work carries an explicit account id and never reads the active UI account (protects against races when switching accounts). Periodic intervals clamped to the platform minimum; lifecycle triggers apply a 15-minute staleness threshold (no aggressive polling).
- Offline state is cleanly distinguished from authentication failure or API errors — never show "Reconnect" when simply offline. Last-sync timestamps are truthful, no fake precision. Notifications are honest (no fake/bait content).
