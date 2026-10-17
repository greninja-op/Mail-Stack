# Phase 28 — Comprehensive Functional & Integration QA

## Mission
Implement and verify only Phase 28. Do not implement later phases.

## Mandatory Windows isolation
Modify only the Windows platform boundary and genuinely shared Rust desktop code required by this phase. Never touch macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling areas.

## Required reading
Read root instructions and execution plan/Windows/README.md, requirements.md, spec.md, design.md and editor-rules.md. Inspect the actual repository and current status before editing.

## Objective
Run end-to-end QA through Phase 27: install/upgrade/restart, OAuth, sync, parsing, categories, companies, search, priority, Action Required, rules, temporal/conversation intelligence, Calendar, Tasks, multi-account, offline, notifications, automation and AI fallback. Fix defects and record only real evidence.

## Rust-first contract
Rust is the authoritative product/core implementation and must remain reusable by macOS. Keep Windows UI Rust-first. Native Windows SDK calls belong behind narrow Rust adapters. Never move business logic into another language merely for convenience.

## Security and trust
Gmail remains cloud source of truth. Preserve account isolation, least-privilege OAuth, secure credentials, deterministic/local/explainable precedence and untrusted-email rules. Consequential external effects require appropriate confirmation and idempotency.

## Verification
Run targeted and integration tests; build and launch the real Windows application when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart, sleep/wake and recovery states as relevant; verify accessibility, keyboard/pointer behavior, security, data integrity, memory/performance and release configuration where applicable; inspect logs; fix defects; rebuild and retest. Never claim evidence that was not actually observed.

## Documentation
Update status accurately and add only genuinely permanent reusable rules. Record blockers honestly.

**STOP AFTER PHASE 28.**

## 2026-10-18 — Current-design update

- End-to-end QA through Phase 27: install/upgrade/restart, OAuth, sync, parsing, categories, companies, search, priority, Action Required, rules, temporal/conversation intelligence, Calendar, Tasks, multi-account, offline, notifications, automation, AI fallback. Fix defects and record only real evidence.
- Never claim a test, device run, screenshot, log, or validation that was not actually performed. Build success alone is never sufficient.
- Cover: light/dark mode, text scaling, keyboard/pointer, malformed email/HTML cases, permission denial, authentication failure/recovery, account-isolation attack, throttle simulation (per-second throttle vs daily quota), logo-pipeline cases (company vs personal vs unknown domain), category-group rendering (one-cavity, vertical centring, accent colors), import-flow cases (progress never backwards, 99% cap, Done gating, error chrome).
- Screenshot review on every major UI change. Jank/frame-time inspection where practical. Regression review of existing screens.
