# Phase 5 — iOS Mailbox, Navigation & Email Viewer

## MANDATORY PLATFORM ISOLATION — READ FIRST

This phase is **iOS / iPhone only**. Put all iOS-specific UI/assets/tests under `iOS/`. Do not touch other platform directories.

## Mission

Implement the core iPhone mailbox experience using SwiftUI and the established product contract.

## Scope

- iPhone navigation shell
- All Inbox, Primary, Promotional, Social, Spam, Starred
- unified inbox rows with source Gmail account identity
- sender/subject/preview/time
- email star state
- category retention
- email detail/thread view
- loading/empty/error/offline/stale states
- pagination/lazy rendering
- sanitized HTML/plain-text rendering
- safe links
- attachment metadata/download boundary if already supported
- spam recovery entry point where Gmail capability exists

No JavaScript execution from email content.

## Visual QA

Verify light/dark mode, Dynamic Type, VoiceOver labels/order, small and large iPhones, long subjects, large threads, missing sender/avatar data, RTL-safe layout where applicable, and network interruptions.

Do not implement company intelligence or advanced search yet.

**STOP AFTER PHASE 5.**

## 2026-10-18 — Current-design update

- **Mail row (exact):** 40pt avatar, sender + time line 1, subject line 2, snippet line 3, min 88pt height. Flat rows on the background — no card, no border, no shadow.
- **Logo pipeline in every row:** company sender → logo (bundled → asset catalog → favicon → letter fallback); personal sender → letter avatar. The row MUST receive the detected sender domain; a missing domain falling back to letters for company mail is a bug. Unified inbox rows add the small receiving-account indicator circle.
- Star on the right. Unread = bold sender/subject + primary-colored time.
- Native iOS scroll indicators (see `design.md` scrollbar behavior if a custom scrollbar is ever built). Pull-to-refresh fires ONLY on a real finger drag — a fast fling back to the top must never trigger it.
- Thread/message view: message reveal 350ms expand + fade, rotating chevron. HTML sanitized; entities fully decoded; `<!DOCTYPE>`/processing instructions never print as text; images never fetched (render as nothing, no per-image placeholder lines). No JavaScript from mail content.
- Tab switches never reload the page. No "Action Required" chip/filter in the Mail list.
