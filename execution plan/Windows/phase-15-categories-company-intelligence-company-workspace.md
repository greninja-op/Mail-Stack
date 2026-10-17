# Phase 15 — Categories, Company Intelligence & Company Workspace

## Mission
Implement and verify only Phase 15. Do not implement later phases.

## Mandatory Windows isolation
All Windows-specific Rust source, UI, tests, configuration and assets remain inside the Windows boundary. Never modify sibling platform areas.

## Required reading
Read root instructions and all Windows master documents. Inspect the actual repository, status, architecture, tests and previous evidence before editing.

## Objective
Implement category semantics and company intelligence in Rust. Company grouping/filtering is inside the selected category; company pinning is separate from message starring and moves pinned companies to the top of that category's company filter.

## Rust-first architecture
Rust owns the domain/application/data/business behavior and is the reusable desktop source of truth for Windows and macOS. UI code consumes Rust-owned state and must not duplicate decisions.

## Product contract
Preserve Gmail source-of-truth, explicit account isolation, category semantics, company-vs-message-star distinction, deterministic/local/explainable precedence and untrusted-email rules.

## Verification
Run targeted unit/integration tests; build and launch the real Windows app when possible; exercise success, empty, loading, error, cancellation, offline, restart and recovery states relevant to this phase; test large datasets where relevant; verify accessibility, keyboard/mouse/pointer interaction, security and data integrity; inspect logs; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status and add only permanent reusable Windows rules.

**STOP AFTER PHASE 15.**

## 2026-10-18 — Current-design update

- Category semantics and company intelligence in Rust. Company grouping/filtering is inside the selected category; company pinning is separate from message starring and moves pinned companies to the top of that category's company filter.
- **Groups are single-cavity cards:** header + members inside ONE rounded card (no gaps/seams). Member rows are full-width inside the card. Click highlight clipped to the rounded shape (clip BEFORE the input gesture).
- **Group header is vertically centred:** icon, text block, chevron all vertically centred (a horizontal line through the card centre passes through all three). Card hugs content — symmetric vertical padding, no dead space at the bottom.
- **Semantic accent colors** from centralized tokens: Needs attention → amber; Promotions → orange; Updates → sky blue; Work & learning → royal blue; Everything else → brand violet. Leading circle tinted accent at 14% alpha; icon is the full accent.
- **Category detail rows use the same row layout AND the same logo pipeline as the Mail list.** No fabricated counts.
