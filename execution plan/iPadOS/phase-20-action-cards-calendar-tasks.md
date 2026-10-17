# Phase 20 — Action Cards, Calendar & Tasks

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Expose safe Action Cards and integration flows. Reuse common Action Engine; implement provenance, deduplication, confirmation levels, permission state, Calendar/EventKit and supported task adapter boundaries, account association, failure/retry and undo where possible. Use tablet detail/inspector space for explanation. Test denied/revoked permissions, duplicate actions, conflicting events/tasks, offline state, account removal and user cancellation. AI cannot bypass safety.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Safe Action Cards + integration flows on the common Action Engine. Provenance, deduplication (`messageId|actionType|targetKey`; thread-dedup on actionType+targetKey), confirmation levels, permission state, Calendar/EventKit + task adapter boundaries, account association, failure/retry, undo where possible. Use the inspector/detail space for explanation.
- **Engine proposes / user confirms / nothing external automatic.** Generation and safety validation are separate layers. Confirming external proposals honestly reports "not connected" — never fake success.
- Calendar/Tasks use the SAME account + tokens via the one sign-in (no per-integration sign-in). Idempotent creation (Calendar: iCalUID + extended-property key + receipt cache + recovery lookup; Tasks: marker-in-notes + execution store + recovery scan).
- Gmail write (mark read/unread, archive, etc.) behind a confirmation gate: fail-closed, account isolation, idempotency pre-check, Gmail-verified-before-local-update, per-item bulk. 403 → incremental-auth upgrade dialog (gmail.modify is the only incremental scope).
- Test denied/revoked permissions, duplicate actions, conflicting events/tasks, offline state, account removal, user cancellation. AI cannot bypass safety.
