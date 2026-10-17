# Phase 27 — Performance, Accessibility & Reliability Hardening

## Mission
Implement and verify only Phase 27. Do not implement later phases.

## Mandatory Windows isolation
Modify only the Windows platform boundary and genuinely shared Rust desktop code required by this phase. Never touch macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling areas.

## Required reading
Read root instructions and execution plan/Windows/README.md, requirements.md, spec.md, design.md and editor-rules.md. Inspect the actual repository and current status before editing.

## Objective
Harden startup, mailbox rendering, scrolling, indexing, sync concurrency, memory, storage, sleep/wake and power behavior. Verify accessibility, keyboard navigation, contrast, text sizing, focus order, localization resilience, crash recovery and state restoration.

## Rust-first contract
Rust is the authoritative product/core implementation and must remain reusable by macOS. Keep Windows UI Rust-first. Native Windows SDK calls belong behind narrow Rust adapters. Never move business logic into another language merely for convenience.

## Security and trust
Gmail remains cloud source of truth. Preserve account isolation, least-privilege OAuth, secure credentials, deterministic/local/explainable precedence and untrusted-email rules. Consequential external effects require appropriate confirmation and idempotency.

## Verification
Run targeted and integration tests; build and launch the real Windows application when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart, sleep/wake and recovery states as relevant; verify accessibility, keyboard/pointer behavior, security, data integrity, memory/performance and release configuration where applicable; inspect logs; fix defects; rebuild and retest. Never claim evidence that was not actually observed.

## Documentation
Update status accurately and add only genuinely permanent reusable rules. Record blockers honestly.

**STOP AFTER PHASE 27.**

## 2026-10-18 — Current-design update

- Harden startup, mailbox rendering, scrolling, indexing, sync concurrency, memory, storage, sleep/wake, power behavior. Accessibility, keyboard navigation, contrast, text sizing, focus order, localization resilience, crash recovery, state restoration.
- Adaptive performance model: lightweight capability profile with tiers (NOT device names) selecting animation complexity, prefetching, image cache limits, background concurrency, sync batch sizes. Never claim 120 FPS because hardware supports 120 Hz. Reduce expensive work before the UI becomes janky. No continuous hardware benchmarking; re-evaluate when conditions change.
- Centralized motion system (Essential / Standard / Expressive / Reduced Motion). Expressive reserved for meaningful moments. Respect reduced-motion + power-saving settings. No animation blocks interaction or sync.
- Accessibility: Narrator labels/traits/focus order, text scaling including large sizes, high contrast, keyboard-only operation, light/dark mode, varied window sizes.
