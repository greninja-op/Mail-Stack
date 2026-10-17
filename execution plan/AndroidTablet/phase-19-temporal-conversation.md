# Phase 19 — Temporal & Conversation Intelligence

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Implement deadline/date/time extraction, meeting detection, timezone/DST handling, ambiguity/UNKNOWN states, quoted/forwarded text handling, waiting-for-reply and conversation state through shared intelligence. Expose contextual information in tablet list/detail. Test ambiguous dates, locale/timezone changes, DST, quoted text, forwarded messages, duplicate threads and missing timestamps.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 19.**

## 2026-10-18 — Current-design update

- Temporal extraction: 14 item types, reference time = message received, ambiguity policy, idempotent, no schema migration for extracted items.
- Conversation intelligence: 7 deterministic states (NO_ACTION, AWAITING_USER_REPLY, AWAITING_OTHER_PARTY, RECENTLY_REPLIED, STALE_CONVERSATION, RESOLVED, UNKNOWN). Explain what appears to be happening without inventing intent; never equate "no reply" with "user forgot". Negative signals (newsletters, notifications, receipts, spam, no-reply) shield against false "awaiting reply". 3-day follow-up candidates; 7-day stale threshold. Status badge pills + expandable intelligence card with timeline & reasons.
- Expose contextual information in list/detail panes. Test ambiguous dates, locale/timezone changes, DST, quoted text, forwarded messages, duplicate threads, and missing timestamps.
