# Phase 11 — Initial Gmail Sync

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Implement first/full synchronization through the shared engine. Use bounded pages, atomic persistence, cancellation/resume, honest progress states and per-account status. Test empty, small and large mailboxes, pagination boundaries, duplicates, partial failure, auth expiry, cancellation and restart. Verify no cross-account leakage.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 11.**

## 2026-10-18 — Current-design update

- Bounded pages, atomic persistence, cancellation/resume, honest progress states, per-account status. Progress from the REAL sync only, never backwards: total fetched ONCE (Gmail profile `messagesTotal`) before the loop, bounded by the per-run max; caps at 99% while running; Done = 100%.
- **Pacing:** token bucket (~10 req/sec) + honor `Retry-After`. Do NOT fire retry LIST calls while Gmail is throttling. **Never silently skip a throttled per-message GET** — retry dropped GETs; a skipped message is silently missing forever.
- Import flow UI (same as phone, card centred with max width on wide screens): conduit arc is NOT progress (pulse travels it); ONLY the Mailstack ring is progress; flying mails re-seed when the list empties; rows = 2 most recently INSERTED mails; Continue gated on Done; Back = "Not now"; error keeps card chrome; card keeps its hardcoded dark palette in both themes. Spam pass runs once per first sync.
- Test empty, small, and large mailboxes, pagination boundaries, duplicates, partial failure, auth expiry, cancellation, and restart. Verify no cross-account leakage.
