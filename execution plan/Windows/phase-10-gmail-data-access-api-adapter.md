# Phase 10 — Gmail Data Access & API Adapter

## Mission
Implement and verify only Phase 10. Do not implement later phases.

## Mandatory Windows isolation
All Windows-specific Rust source, UI, tests, configuration and assets remain inside the Windows boundary. Never modify sibling platform areas.

## Required reading
Read root instructions and all Windows master documents. Inspect the actual repository, status, architecture, tests and previous evidence before editing.

## Objective
Implement the Gmail adapter in Rust for mailbox metadata, messages, threads, labels, history where supported, pagination, rate limits, auth failures, cancellation, retry and normalization into shared Rust models.

## Rust-first architecture
Rust owns the domain/application/data/business behavior and is the reusable desktop source of truth for Windows and macOS. UI code consumes Rust-owned state and must not duplicate decisions.

## Product contract
Preserve Gmail source-of-truth, explicit account isolation, category semantics, company-vs-message-star distinction, deterministic/local/explainable precedence and untrusted-email rules.

## Verification
Run targeted unit/integration tests; build and launch the real Windows app when possible; exercise success, empty, loading, error, cancellation, offline, restart and recovery states relevant to this phase; test large datasets where relevant; verify accessibility, keyboard/mouse/pointer interaction, security and data integrity; inspect logs; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status and add only permanent reusable Windows rules.

**STOP AFTER PHASE 10.**

## 2026-10-18 — Current-design update

- Gmail adapter in Rust: mailbox metadata, messages, threads, labels, history where supported, pagination, rate limits, auth failures, cancellation, retry, normalization into shared Rust models.
- **Daily quota is effectively never the problem** (one 500-message first sync ≈ 2,682 units vs 1,000,000,000 daily). The real limit is the **per-second throttle (250 units/user/sec)**. Pace requests (token bucket, ~10 req/sec) + honor Gmail's `Retry-After` header. **Never silently skip a throttled per-message GET** — retry dropped GETs; a skipped message is silently missing forever. Do NOT fire retry LIST calls while Gmail is throttling (they extend the throttle).
- Parse the FULL error body before classifying (never truncate-then-parse — truncation mid-JSON hides the real reason). Map Gmail's actual scope-error reason (`insufficientScopes`) and `dailyLimitExceeded`.
