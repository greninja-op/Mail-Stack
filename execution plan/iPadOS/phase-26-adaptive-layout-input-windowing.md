# Phase 26 — Adaptive Layout, Pointer, Keyboard & Stage Manager

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Perform the dedicated interaction/layout pass across the entire product. Verify every feature in narrow/medium/wide windows, portrait/landscape, split view and Stage Manager. Refine column thresholds, selection restoration, pointer hover, keyboard shortcuts, focus traversal, contextual menus, drag/drop where useful and window resize transitions. No feature may depend on fixed device resolution. Test rapid resizing during loading/sync/search/detail transitions and accessibility focus stability.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Dedicated interaction/layout pass across the ENTIRE product. Verify every feature in narrow/medium/wide windows, portrait/landscape, split view, Slide Over, and Stage Manager.
- Refine column thresholds (content-driven, never device-name checks), selection restoration, pointer hover, keyboard shortcuts, focus traversal, contextual menus, drag/drop where useful, window resize transitions. No feature may depend on fixed device resolution.
- **Bars:** scroll DOWN hides, scroll UP shows; pure translate, 250ms; entering a tab/column always shows bars. **Settings** slides in from the right (350ms, underlying content slides 1/12 left); closing reverses it.
- Test rapid resizing during loading/sync/search/detail transitions and accessibility focus stability. The iPhone-like narrow fallback must behave exactly like the iPhone contract (same rows, same groups, same rules).
