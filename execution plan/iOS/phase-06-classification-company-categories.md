# Phase 6 — iOS Classification, Company Intelligence & Categories

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. iOS-specific UI/config/tests/docs stay in `iOS/`. No sibling platform work.

## Mission

Expose the existing deterministic intelligence systems through an iPhone-native category and company experience.

## Scope

- category presentation
- deterministic classification results
- sender/company intelligence
- company grouping within a selected category
- company filter UI
- pinned companies at top of the category company filter
- distinction between company pin and email star
- Promotional/Social organization
- Spam presentation and recovery
- classification explanations where supported
- user correction entry points needed by later phases

Do not create an iOS-only classifier.

## Validation

Use synthetic/adversarial senders and malformed metadata. Verify unknown/ambiguous classification remains safe. Verify company grouping does not leak across Gmail accounts.

**STOP AFTER PHASE 6.**

## 2026-10-18 — Current-design update

- **Groups are single-cavity cards:** header + members inside ONE rounded card (no gaps/seams). Member rows are full-width inside the card. Tap highlight clipped to the rounded shape (clip BEFORE the tap gesture).
- **Group header is vertically centred:** icon, text block, chevron all vertically centred (a horizontal line through the card centre passes through all three). Card hugs content — symmetric vertical padding, no dead space at the bottom.
- **Semantic accent colors** from centralized tokens: Needs attention → amber; Promotions → orange; Updates → sky blue; Work & learning → royal blue; Everything else → brand violet. Leading circle tinted accent at 14% alpha; icon is the full accent.
- **Category detail rows use the same row layout AND the same logo pipeline as the Mail tab.** The detail row model must carry the raw sender email + display name so the domain can be detected.
- Company grouping/filtering inside the selected category; pinning separate from starring; no fabricated counts.
