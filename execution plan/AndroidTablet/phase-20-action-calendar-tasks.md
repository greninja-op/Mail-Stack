# Phase 20 — Action Cards, Calendar & Tasks

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Expose safe Action Cards and Calendar/Tasks flows. Reuse common Action Engine, provenance, idempotency, confirmation and account association. Use tablet detail/inspector space for explanation. Test permissions, denial/revocation, duplicate actions, conflicts, offline state, cancellation and account removal. AI cannot bypass action safety.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 20.**

## 2026-10-18 — Current-design update

- Action Engine: proposes / user confirms / nothing external automatic. Generation and safety validation are separate layers. Deterministic dedup id `messageId|actionType|targetKey`; thread-dedup on (actionType, targetKey). Confirming external proposals honestly reports "not connected" — never fake success.
- Calendar/Tasks use the SAME account + tokens via the one sign-in (no per-integration sign-in). Idempotent creation (Calendar: iCalUID + extended-property key + receipt cache + recovery lookup; Tasks: marker-in-notes + execution store + recovery scan).
- Gmail write behind a confirmation gate: fail-closed, account isolation, idempotency pre-check, Gmail-verified-before-local-update, per-item bulk. 403 → incremental-auth upgrade dialog (gmail.modify is the only incremental scope).
- Use detail/inspector space for explanation. Test permissions, denial/revocation, duplicate actions, conflicts, offline state, cancellation, and account removal. AI cannot bypass action safety.
