# Phase 14 — macOS Mail Workspace & Conversation Viewer

## Mission
Implement and verify only Phase 14. Do not implement later phases.

## Isolation
Modify only macOS-specific code and genuinely shared KMP code required by this phase. Never modify iOS, iPadOS, AndroidTablet, Android, Windows, Linux, or other sibling areas.

## Read first
Read root instructions and all files in `execution plan/macOS/` master docs. Inspect the actual repository, status, architecture, existing tests, and previous evidence before editing.

## Objective
Build the native desktop mail list/detail and conversation viewer with selection, thread navigation, safe HTML rendering, attachment metadata, loading/empty/error/offline states, keyboard movement, context actions, and large-thread performance.

## Product rules
Preserve Gmail as source of truth, explicit account boundaries, category semantics, and deterministic/local/explainable behavior. Individual email star is independent from company pinning. Do not silently delete or hide promotional/social mail. Do not make companies a replacement for mailbox navigation.

## Verification
Run targeted automated tests; build and run the actual macOS app when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart and recovery states; test large datasets where relevant; verify accessibility, keyboard/pointer behavior, data integrity and account isolation; inspect logs; fix defects; rebuild and retest. Never claim unrun evidence.

## Documentation
Update status accurately and add only genuinely permanent rules. Do not delete unrelated rules.

**STOP AFTER PHASE 14.**

## 2026-10-18 — Current-design update

- **Mail row (exact):** 40pt avatar, sender + time line 1, subject line 2, snippet line 3, min 88pt height. Flat rows on the background — no card, no border, no shadow.
- **Logo pipeline in every row:** company sender → logo (bundled → asset catalog → favicon → letter fallback); personal sender → letter avatar. Unified inbox rows add the small receiving-account indicator circle. Star on the right. Unread = bold sender/subject + primary-colored time.
- Native macOS scroll indicators. Refresh is an explicit user action (toolbar button / Cmd+R) — never auto-fire on scroll.
- Conversation viewer: message reveal 350ms expand + fade, rotating chevron. HTML sanitized; entities decoded; images never fetched (no per-image placeholder lines). No JavaScript from mail content.
- Keyboard movement (up/down, Cmd+arrows, J/K), context actions, selection restoration, large-thread performance. No "Action Required" chip/filter in the Mail list.
