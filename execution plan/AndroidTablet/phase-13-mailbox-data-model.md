# Phase 13 — Mailbox Data Model & Message Presentation

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Build tablet view models over synchronized shared data. Present sender, subject, preview, time, receiving account, unread/star state and labels. Implement sanitized HTML/plain text, safe URLs, attachment metadata and thread navigation. Test malformed HTML, hostile links, missing headers, huge threads, deleted messages and accessibility semantics.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 13.**

## 2026-10-18 — Current-design update

- View models over synchronized shared data. **Mail row (exact):** 40dp avatar, sender + time line 1, subject line 2, snippet line 3, min 88dp height. Flat rows on the background.
- **Logo pipeline in every row:** company sender → logo (bundled → ICONS-ASSETS → favicon → letter fallback); personal sender → letter avatar. The row MUST receive the detected sender domain; a missing domain falling back to letters for company mail is a bug. Unified rows add the small receiving-account indicator circle. Star on the right. Unread = bold sender/subject + primary-colored time.
- Sanitized HTML/plain text: entities fully decoded (no raw `&bull;`, no zero-width-character walls); `<!DOCTYPE>`/processing instructions/CDATA never print as text; images never fetched (render as nothing, no per-image placeholder lines). Safe URLs. Attachment metadata. Thread navigation.
- Test malformed HTML, hostile links, missing headers, huge threads, deleted messages, and accessibility semantics.
