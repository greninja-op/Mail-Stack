# Phase 13 — Mailbox Data Model & Message Presentation

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Build the iPad mailbox data/presentation layer over synchronized shared data. Define list/detail view models without duplicating domain logic. Render sender, subject, preview, date/time, receiving Gmail account, unread state, star state and relevant labels. Implement sanitized HTML/plain-text message rendering, safe URL handling, attachment metadata boundaries and thread navigation. Test long subjects, malformed HTML, missing sender data, huge threads, deleted messages, empty states and VoiceOver. Do not implement advanced category/company intelligence yet.

## ACCEPTANCE / VALIDATION

Inspect the actual repository and prior evidence. Implement only this phase. Run targeted automated tests, then iPad Simulator and real iPad validation where available. Test multiple viewport states relevant to the feature and exercise failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- **Mail row (exact):** 40pt avatar, sender + time line 1, subject line 2, snippet line 3, min 88pt height. Flat rows on the background — no card, no border, no shadow. Wide list columns may ADD category/company/priority/attachment/conversation-state metadata inline — the core row never changes shape.
- **Logo pipeline in every row:** company sender → logo (bundled → asset catalog → favicon → letter fallback); personal sender → letter avatar. The row MUST receive the detected sender domain; a missing domain falling back to letters for company mail is a bug. Unified inbox rows add the small receiving-account indicator circle.
- Star on the right. Unread = bold sender/subject + primary-colored time. No fabricated unread counts.
- Thread/message rendering: message reveal 350ms expand + fade, rotating chevron. HTML sanitized; entities fully decoded; `<!DOCTYPE>`/processing instructions never print as text; images never fetched (render as nothing, no per-image placeholder lines). No JavaScript from mail content. Safe links only.
- Pull-to-refresh fires ONLY on a real finger drag — a fast fling back to the top must never trigger it. Native scroll indicators (see `design.md` scrollbar behavior if a custom scrollbar is ever built).
- Test long subjects, malformed HTML, missing sender data, huge threads, deleted messages, empty states, and VoiceOver. Do not implement advanced category/company intelligence yet.
