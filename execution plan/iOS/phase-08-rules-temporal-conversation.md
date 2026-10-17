# Phase 8 — iOS Rules, Corrections, Temporal & Conversation Intelligence

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target is **iOS / iPhone only**. iOS-specific UI and platform artifacts remain in `iOS/`. Do not implement sibling platforms.

## Mission

Expose user corrections/rules and the existing temporal/conversation intelligence on iPhone without duplicating shared engines.

## Scope

- user classification corrections
- rule creation/edit/disable/delete UI
- precedence and explainability
- deadline extraction/presentation
- meeting detection
- waiting-for-reply/conversation state
- thread-level summaries/states where supported
- stale/unknown handling
- account-scoped rules
- rule history/audit where architecture supports it

Rules must be deterministic and bounded. Email content cannot silently create rules or authorize actions.

## Validation

Test contradictory rules, user correction precedence, ambiguous dates, timezone/DST boundaries, forwarded content, quoted text, duplicate threads, deleted messages, and account isolation.

**STOP AFTER PHASE 8.**

## 2026-10-18 — Current-design update

- Precedence: User Correction > User Rule > Deterministic > Optional AI > Unknown. Undo deletes correction rows so engines restore the base (never stored twice). Rule precedence = (order, id), first match per action type wins; empty-condition rules never match; conflicts detected by same-condition-signature + different values.
- Conversation intelligence: 7 deterministic states (NO_ACTION, AWAITING_USER_REPLY, AWAITING_OTHER_PARTY, RECENTLY_REPLIED, STALE_CONVERSATION, RESOLVED, UNKNOWN). Explain what appears to be happening without inventing intent; never equate "no reply" with "user forgot". Negative signals (newsletters, notifications, receipts, spam, no-reply) shield against false "awaiting reply". 3-day follow-up candidates; 7-day stale threshold. Status badge pills + expandable intelligence card with timeline & reasons.
- Temporal extraction: 14 item types, reference time = message received, ambiguity policy, idempotent, no schema migration for extracted items.
