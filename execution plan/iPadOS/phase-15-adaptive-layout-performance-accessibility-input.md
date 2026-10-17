# Phase 15 — Categories, Company Intelligence & Company Workspace

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Expose deterministic category/company intelligence in the iPad workspace. Implement All Inbox, Primary, Promotional, Social, Spam and Starred views using shared classification. Build a company context/filter area that remains within the selected category. Company pinning moves companies to the top of the company filter and is distinct from email starring. Verify account-scoped company grouping, unknown senders, ambiguous domains, classification explanations, category transitions and selection preservation across list/detail columns. Test adversarial sender metadata and cross-account leakage.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior evidence. Implement only this phase. Run targeted unit/integration tests plus iPad Simulator and real iPad validation where available. Test multiple viewport modes, accessibility and relevant failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Deterministic category/company intelligence in the workspace. All Inbox, Primary, Promotional, Social, Spam, Starred via shared classification.
- **Groups are single-cavity cards:** header + members inside ONE rounded card (no gaps/seams). Member rows full-width inside the card. Tap highlight clipped to the rounded shape. **Group header vertically centred** (icon, text, chevron — a horizontal line through the card centre passes through all three). Card hugs content, symmetric vertical padding.
- **Semantic accent colors:** Needs attention → amber; Promotions → orange; Updates → sky blue; Work & learning → royal blue; Everything else → brand violet. Leading circle tinted accent at 14% alpha; icon is the full accent.
- **Category detail rows use the same row layout AND logo pipeline as the Mail list.** The detail row model must carry the raw sender email + display name so the domain can be detected.
- Company context/filter area stays WITHIN the selected category (its own column/region at wide widths, collapsed into the category view at narrow widths — never a top-level destination). Pinning moves companies to the top of the company filter and is distinct from email starring. Verify account-scoped grouping, unknown senders, ambiguous domains, classification explanations, category transitions, selection preservation across columns. Test adversarial sender metadata and cross-account leakage.
