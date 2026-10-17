# Phase 14 — Tablet Mail Workspace & Conversation Viewer

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Complete the tablet mail workspace: category/navigation context + persistent message list + selected conversation/detail where width permits. Preserve list context, support thread navigation, attachment presentation, loading/stale/offline/error states and selection restoration. Validate narrow split fallback, wide panes, orientation, resizing, keyboard/mouse/touch and large threads.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 14.**

## 2026-10-18 — Current-design update

- Category/navigation context + persistent message list + selected conversation/detail where width permits. Preserve list context and selection across pane changes, rotation, and resizing.
- Thread navigation, attachment presentation, loading/stale/offline/error states, selection restoration. Message reveal 350ms expand + fade, rotating chevron.
- Material scrollbar on the list pane: invisible until scroll; slide+fade in 200ms; out after 1.5s static; thumb-only growth 4dp→12dp on press-and-hold; drag = fast-scroll; track press jumps the thumb; no scrollbar when content fits.
- Pull-to-refresh fires ONLY on a real finger drag — a fast fling back to the top must never trigger it. Mouse-wheel scroll must never trigger pull-to-refresh either.
- Validate narrow split fallback, wide panes, orientation, resizing, keyboard/mouse/touch, and large threads.
