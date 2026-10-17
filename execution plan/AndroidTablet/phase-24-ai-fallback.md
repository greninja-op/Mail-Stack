# Phase 24 — Optional AI Fallback

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Integrate existing optional AI architecture: deterministic-first routing, confidence, structured validation, prompt-injection defense, minimal context, sensitive-data controls, provider disclosure, enable/disable, failure/offline fallback and provenance. AI cannot authorize Gmail/Calendar/Tasks/automation/external actions. Test disabled/unavailable/malformed/adversarial/timeout/offline cases.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 24.**

## 2026-10-18 — Current-design update

- AI is an optional secondary fallback, never the foundation. Deterministic local intelligence remains authoritative. AI disabled by default with explicit user opt-in. AI is never required for core mailbox access, search, classification, or safety.
- DataMinimizer: strip HTML, MIME headers, recipient lists; truncate bodies to ≤500 characters; sanitize identifiers (emails/tokens). Prompt boundary: anti-injection delimiter shielding.
- Output validator: strict structured JSON schemas, model confidence capped at 0.85, reject executable command verbs (delete/send/drop), and NEVER downgrade deterministic SECURITY classifications. AI output never directly executes actions.
- On-demand thread summarization is explicitly user-initiated with a clear advisory notice ("Advisory summary · May contain errors"). Subtle "AI suggestion" badge pill where a suggestion appears.
- Test disabled/unavailable/malformed/adversarial/timeout/offline cases.
