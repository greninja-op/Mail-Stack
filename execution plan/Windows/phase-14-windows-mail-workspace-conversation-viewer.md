# Phase 14 — Windows Mail Workspace & Conversation Viewer

## Mission
Implement and verify only Phase 14. Do not implement later phases.

## Mandatory Windows isolation
All Windows-specific Rust source, UI, tests, configuration and assets remain inside the Windows boundary. Never modify sibling platform areas.

## Required reading
Read root instructions and all Windows master documents. Inspect the actual repository, status, architecture, tests and previous evidence before editing.

## Objective
Build the Rust-first native Windows mail list/detail and conversation viewer. Cover selection, thread navigation, safe HTML rendering, attachment metadata, loading/empty/error/offline states, keyboard/pointer movement, context actions and large-thread performance.

## Rust-first architecture
Rust owns the domain/application/data/business behavior and is the reusable desktop source of truth for Windows and macOS. UI code consumes Rust-owned state and must not duplicate decisions.

## Product contract
Preserve Gmail source-of-truth, explicit account isolation, category semantics, company-vs-message-star distinction, deterministic/local/explainable precedence and untrusted-email rules.

## Verification
Run targeted unit/integration tests; build and launch the real Windows app when possible; exercise success, empty, loading, error, cancellation, offline, restart and recovery states relevant to this phase; test large datasets where relevant; verify accessibility, keyboard/mouse/pointer interaction, security and data integrity; inspect logs; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status and add only permanent reusable Windows rules.

**STOP AFTER PHASE 14.**

## 2026-10-18 — Current-design update

- Rust-first native Windows mail list/detail + conversation viewer. Selection, thread navigation, attachment metadata, loading/empty/error/offline states, keyboard/pointer movement, context actions, large-thread performance.
- **Mail row (exact):** 40px avatar, sender + time line 1, subject line 2, snippet line 3, min 88px height. Flat rows on the background — no card, no border, no shadow.
- **Logo pipeline in every row:** company sender → logo (bundled → app assets → favicon → letter fallback); personal sender → letter avatar. The row MUST receive the detected sender domain; a missing domain falling back to letters for company mail is a bug. Unified inbox rows add the small receiving-account indicator circle. Star on the right. Unread = bold sender/subject + primary-colored time.
- Thread/message view: message reveal 350ms expand + fade, rotating chevron. HTML sanitized; entities fully decoded; `<!DOCTYPE>`/processing instructions never print as text; images never fetched (render as nothing, no per-image placeholder lines). No JavaScript from mail content.
- Refresh is explicit (button/shortcut) — a fast fling or velocity gesture must never trigger a refresh. No "Action Required" chip/filter in the Mail list.
