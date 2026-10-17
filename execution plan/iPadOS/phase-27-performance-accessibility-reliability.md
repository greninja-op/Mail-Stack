# Phase 27 — Performance, Accessibility & Reliability Hardening

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Profile real workflows: cold/warm launch, scrolling, search, sync, thread opening, account switching, resizing and background/foreground. Fix SwiftUI state churn, unbounded lists, image/cache pressure, memory leaks, excessive network work and expensive background tasks. Complete VoiceOver, Dynamic Type, Reduce Motion, contrast, focus and keyboard/pointer accessibility. Test Low Power Mode, memory pressure and large datasets. ProMotion is not a 120 FPS guarantee.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Adaptive performance model: lightweight capability profile with tiers (NOT device names) selecting animation complexity, prefetching, image cache limits, background concurrency, sync batch sizes. Never claim 120 FPS because hardware supports ProMotion/120 Hz. Reduce expensive work before the UI becomes janky. No continuous hardware benchmarking; re-evaluate when conditions change.
- Centralized motion system (Essential / Standard / Expressive / Reduced Motion). Expressive reserved for meaningful moments. Respect Reduce Motion + Low Power Mode. No animation blocks interaction or sync.
- Profile real workflows: cold/warm launch, scrolling, search, sync, thread opening, account switching, resizing, background/foreground. Fix SwiftUI state churn, unbounded lists, image/cache pressure, memory leaks, excessive network work, expensive background tasks.
- Complete VoiceOver, Dynamic Type, Reduce Motion, contrast, focus, keyboard/pointer accessibility. Test Low Power Mode, memory pressure, large datasets. ProMotion is not a 120 FPS guarantee.
