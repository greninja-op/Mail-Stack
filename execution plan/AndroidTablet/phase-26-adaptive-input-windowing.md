# Phase 26 — Adaptive Layout, Pointer, Keyboard & Multi-Window

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Run a dedicated whole-product large-screen interaction pass. Verify narrow/medium/wide panes, portrait/landscape, split-screen and multi-window. Refine adaptive thresholds, selection restoration, mouse/pointer hover, keyboard shortcuts/focus, context menus and useful drag/drop. Test rapid resizing during sync/search/loading/detail transitions and accessibility focus stability.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 26.**

## 2026-10-18 — Current-design update

- Dedicated whole-product large-screen interaction pass: narrow/medium/wide panes, portrait/landscape, split-screen, multi-window. Refine adaptive thresholds (content-driven, not device-name checks), selection restoration, mouse/pointer hover, keyboard shortcuts/focus, context menus, and useful drag & drop.
- Verify rapid resizing during sync/search/loading/detail transitions and accessibility focus stability. Stylus where genuinely useful; touch remains first-class.
- Re-verify the current motion specs at every width: tab-switch slide (no reload), Settings slide/pane, scroll-hide bars (250ms translate), message reveal (350ms), scrollbar behavior.
