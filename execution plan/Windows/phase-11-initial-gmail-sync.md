# Phase 11 — Initial Gmail Sync

## Mission
Implement and verify only Phase 11. Do not implement later phases.

## Mandatory Windows isolation
All Windows-specific Rust source, UI, tests, configuration and assets remain inside the Windows boundary. Never modify sibling platform areas.

## Required reading
Read root instructions and all Windows master documents. Inspect the actual repository, status, architecture, tests and previous evidence before editing.

## Objective
Implement first sync with discovery, retrieval, normalization, persistence, classification handoff, index population, honest progress/indeterminate state, cancellation, retry, partial failure and resumability.

## Rust-first architecture
Rust owns the domain/application/data/business behavior and is the reusable desktop source of truth for Windows and macOS. UI code consumes Rust-owned state and must not duplicate decisions.

## Product contract
Preserve Gmail source-of-truth, explicit account isolation, category semantics, company-vs-message-star distinction, deterministic/local/explainable precedence and untrusted-email rules.

## Verification
Run targeted unit/integration tests; build and launch the real Windows app when possible; exercise success, empty, loading, error, cancellation, offline, restart and recovery states relevant to this phase; test large datasets where relevant; verify accessibility, keyboard/mouse/pointer interaction, security and data integrity; inspect logs; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status and add only permanent reusable Windows rules.

**STOP AFTER PHASE 11.**

## 2026-10-18 — Current-design update

- First sync: discovery, retrieval, normalization, persistence, classification handoff, index population, cancellation, retry, partial failure, resumability.
- **Progress from the real sync only, never backwards:** total fetched ONCE (Gmail profile `messagesTotal`) before the loop, bounded by the per-run max; caps at 99% while running; Done = 100%. Indeterminate state only when no trustworthy denominator exists.
- Import-flow UI (see `design.md`): conduit arc is NOT a progress bar (a pulse travels it); ONLY the Mailstack ring is progress. Real Gmail + Mailstack logos (user-supplied, as-is). Flying mails stay visible (re-seed whenever the list empties while progress < 100%). Rows show the 2 most recently INSERTED mails. Continue gated on Done. Back = "Not now". Error variant keeps card chrome (Try again / Continue anyway). Card keeps its hardcoded dark palette in both themes. Spam pass runs once per first sync.
