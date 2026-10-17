# Phase 15 — Categories, Company Intelligence & Company Workspace

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Expose All Inbox, Primary, Promotional, Social, Spam and Starred using shared classification. Add company filtering within the selected category, company pinning at the top of company filters, and clear distinction between company pin and email star. Test unknown/ambiguous companies, account isolation, category transitions, adversarial sender metadata and selection preservation.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 15.**

## 2026-10-18 — Current-design update

- **Groups are single-cavity cards:** header + members inside ONE rounded card (no gaps/seams). Member rows are full-width inside the card. Tap highlight clipped to the rounded shape (clip BEFORE clickable).
- **Group header is vertically centred:** icon, text block, chevron all vertically centred (a horizontal line through the card centre passes through all three). Card hugs content — symmetric vertical padding, no dead space at the bottom.
- **Semantic accent colors** from centralized tokens: Needs attention → amber; Promotions → orange; Updates → sky blue; Work & learning → royal blue; Everything else → brand violet. Leading circle tinted accent at 14% alpha; icon is the full accent.
- **Category detail rows use the same row layout AND the same logo pipeline as the Mail list.** The detail row model must carry the raw sender email + display name so the domain can be detected.
- Company filtering within the selected category; company pinning at the top of company filters; clear distinction between company pin and email star. Test unknown/ambiguous companies, account isolation, category transitions, adversarial sender metadata, and selection preservation.
