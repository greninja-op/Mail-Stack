# Phase 9 — iOS Action Engine, Calendar & Tasks

## MANDATORY PLATFORM ISOLATION — READ FIRST

This phase targets **iOS / iPhone only**. Keep iOS-specific integrations and UI under `iOS/`. Do not touch other platform plans.

## Mission

Expose safe Action Cards and integrate calendar/task capabilities through explicit, user-controlled boundaries.

## Scope

- Action Cards
- action explanations/provenance
- deduplication/idempotency
- confirmation levels
- Calendar integration abstraction
- iOS Calendar/EventKit adapter where required
- task integration abstraction and supported iOS task target
- permission states
- account association
- failure/retry
- undo/recovery where possible

No email may authorize an external action. AI is not allowed to bypass the Action Engine.

## Validation

Test permission denial, revoked permission, duplicate action attempts, conflicting calendar/task state, offline behavior, account removal, and user cancellation.

**STOP AFTER PHASE 9.**

## 2026-10-18 — Current-design update

- Action Engine: proposes / user confirms / nothing external automatic. Generation and safety validation are separate layers. Deterministic dedup id `messageId|actionType|targetKey`; thread-dedup on (actionType, targetKey). Confirming external proposals honestly reports "not connected" — never fake success.
- Calendar/Tasks use the SAME account + tokens via the one sign-in (no per-integration sign-in). Idempotent creation (Calendar: iCalUID + extended-property key + receipt cache + recovery lookup; Tasks: marker-in-notes + execution store + recovery scan).
- Gmail write (mark read/unread, archive, etc.) behind a confirmation gate: fail-closed, account isolation, idempotency pre-check, Gmail-verified-before-local-update, per-item bulk. 403 → incremental-auth upgrade dialog (gmail.modify is the only incremental scope).
