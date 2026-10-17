# Phase 15 — Categories, Company Intelligence & Company Workspace

## Mission
Implement and verify only Phase 15. Do not implement later phases.

## Isolation
Modify only macOS-specific code and genuinely shared KMP code required by this phase. Never modify iOS, iPadOS, AndroidTablet, Android, Windows, Linux, or other sibling areas.

## Read first
Read root instructions and all files in `execution plan/macOS/` master docs. Inspect the actual repository, status, architecture, existing tests, and previous evidence before editing.

## Objective
Implement category behavior and company intelligence. Company grouping/filtering lives inside the selected category; company pinning is distinct from email starring and moves pinned companies to the top of the category company filter. Normalize senders/domains and handle ambiguity.

## Product rules
Preserve Gmail as source of truth, explicit account boundaries, category semantics, and deterministic/local/explainable behavior. Individual email star is independent from company pinning. Do not silently delete or hide promotional/social mail. Do not make companies a replacement for mailbox navigation.

## Verification
Run targeted automated tests; build and run the actual macOS app when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart and recovery states; test large datasets where relevant; verify accessibility, keyboard/pointer behavior, data integrity and account isolation; inspect logs; fix defects; rebuild and retest. Never claim unrun evidence.

## Documentation
Update status accurately and add only genuinely permanent rules. Do not delete unrelated rules.

**STOP AFTER PHASE 15.**

## 2026-10-18 — Current-design update

- **Groups are single-cavity cards:** header + members inside ONE rounded card (no gaps/seams). Member rows are full-width inside the card. Tap highlight clipped to the rounded shape.
- **Group header is vertically centred:** icon, text block, chevron all vertically centred (a horizontal line through the card centre passes through all three). Card hugs content — symmetric vertical padding, no dead space at the bottom.
- **Semantic accent colors** from centralized tokens: Needs attention → amber; Promotions → orange; Updates → sky blue; Work & learning → royal blue; Everything else → brand violet. Leading circle tinted accent at 14% alpha; icon is the full accent.
- **Category detail rows use the same row layout AND the same logo pipeline as the Mail list.** The detail model must carry the raw sender email + display name so the domain can be detected.
- Company grouping/filtering inside the selected category; pinning separate from starring; no fabricated counts. Company workspace shows the filtered category view with the category context visible.
