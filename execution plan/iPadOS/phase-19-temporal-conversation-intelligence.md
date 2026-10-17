# Phase 19 — Temporal Intelligence & Conversation Intelligence

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Implement deadline/date/time extraction and meeting detection through shared intelligence. Add timezone/DST handling, ambiguity/UNKNOWN states, quoted/forwarded content handling, recurring dates, waiting-for-reply and conversation state. Expose these in iPad list/detail/workspace context without duplicating engines. Test ambiguous dates, locale/timezone changes, DST boundaries, quoted text, forwarded mail, duplicate threads, edited messages and missing timestamps.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Deadline/date/time extraction + meeting detection through shared intelligence. Timezone/DST handling, ambiguity/UNKNOWN states, quoted/forwarded content handling, recurring dates, waiting-for-reply, conversation state.
- 14 temporal item types; reference time = message received; ambiguity policy; idempotent; no schema migration for extracted items.
- **7 conversation states:** NO_ACTION, AWAITING_USER_REPLY, AWAITING_OTHER_PARTY, RECENTLY_REPLIED, STALE_CONVERSATION, RESOLVED, UNKNOWN. Explain what appears to be happening without inventing intent; never equate "no reply" with "user forgot". Negative signals (newsletters, notifications, receipts, spam, no-reply) shield against false "awaiting reply". 3-day follow-up candidates; 7-day stale threshold.
- Expose in list/detail/inspector context (status badge pills + expandable intelligence card with timeline & reasons) without duplicating engines. Test ambiguous dates, locale/timezone changes, DST boundaries, quoted text, forwarded mail, duplicate threads, edited messages, missing timestamps.
