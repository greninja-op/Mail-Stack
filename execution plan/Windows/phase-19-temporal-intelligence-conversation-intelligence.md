# Phase 19 — Temporal Intelligence & Conversation Intelligence

## Mission
Implement and verify only Phase 19. Do not implement later phases.

## Mandatory Windows isolation
All Windows-specific Rust source, UI, tests, configuration and assets remain inside the Windows boundary. Never modify sibling platform areas.

## Required reading
Read root instructions and all Windows master documents. Inspect the actual repository, status, architecture, tests and previous evidence before editing.

## Objective
Implement Rust temporal and conversation intelligence for deadlines, meetings, waiting-for-reply, follow-up and thread state. Handle timezone/date ambiguity, quoted text, duplicates, malformed content and user corrections while keeping extracted facts distinct from confirmed external objects.

## Rust-first architecture
Rust owns the domain/application/data/business behavior and is the reusable desktop source of truth for Windows and macOS. UI code consumes Rust-owned state and must not duplicate decisions.

## Product contract
Preserve Gmail source-of-truth, explicit account isolation, category semantics, company-vs-message-star distinction, deterministic/local/explainable precedence and untrusted-email rules.

## Verification
Run targeted unit/integration tests; build and launch the real Windows app when possible; exercise success, empty, loading, error, cancellation, offline, restart and recovery states relevant to this phase; test large datasets where relevant; verify accessibility, keyboard/mouse/pointer interaction, security and data integrity; inspect logs; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status and add only permanent reusable Windows rules.

**STOP AFTER PHASE 19.**

## 2026-10-18 — Current-design update

- Rust temporal and conversation intelligence for deadlines, meetings, waiting-for-reply, follow-up, thread state. Timezone/date ambiguity, quoted text, duplicates, malformed content, user corrections; extracted facts stay distinct from confirmed external objects.
- Temporal extraction: 14 item types, reference time = message received, ambiguity policy, idempotent.
- Conversation intelligence: 7 deterministic states (NO_ACTION, AWAITING_USER_REPLY, AWAITING_OTHER_PARTY, RECENTLY_REPLIED, STALE_CONVERSATION, RESOLVED, UNKNOWN). Explain what appears to be happening without inventing intent; never equate "no reply" with "user forgot". Negative signals (newsletters, notifications, receipts, spam, no-reply) shield against false "awaiting reply". 3-day follow-up candidates; 7-day stale threshold. Status badge pills + expandable intelligence card with timeline & reasons.
