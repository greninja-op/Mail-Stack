# Phase 00 — Architecture, Shared-Core & Large-Screen Audit

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Audit actual repository and classify Android phone/tablet/KMP components EXISTING/PARTIAL/MISSING/DUPLICATE/UNSAFE. Map shared core versus tablet presentation. Audit Gradle modules, Compose, dependencies, window APIs, persistence, OAuth, sync, notifications, Calendar/Tasks, tests and build tooling. Profile information density across portrait, landscape, split-screen and common tablet widths. Define content-driven adaptive layout states. Establish permanent folder/isolation rule. Baseline build only; no later features.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 00.**

## 2026-10-18 — Current-design update

- Audit against the CURRENT phone UI (1.0.39 + in-main fixes), not the pre-2026-10-18 plan docs. Record the design delta in `design.md` / `editor-rules.md` as the target the tablet build must match.
- Classify the shared KMP/Android code against the current behavior: the shared layer already carries the current sync/classification/search/logo/throttle semantics; the tablet work is presentation + platform boundary, not re-derivation of the design.
- Verify the tablet target can consume: the logo pipeline (bundled → ICONS-ASSETS → favicon → letter), the fixed-denominator progress model, the cursor-resume sync model, and the per-second-throttle pacing model.
- Profile information density across portrait, landscape, split-screen, and common tablet widths against the CURRENT row spec (40dp avatar, 3 lines, 88dp min) — density comes from panes, not from a second row design.
- Do not re-derive the design from the old plan docs. Where the old plan and `design.md` (2026-10-18) disagree, `design.md` wins.
