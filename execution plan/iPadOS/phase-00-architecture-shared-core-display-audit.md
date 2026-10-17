# Phase 0 — iPadOS Architecture, Shared-Core & Display Audit

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific implementation, tests and documents belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Audit the actual repository before implementation. Inventory KMP shared modules, Android, iOS and any existing iPad target; classify relevant components EXISTING/PARTIAL/MISSING/DUPLICATE/UNSAFE. Map exactly which source files must remain common and which presentation/OS integrations belong to iPadOS. Audit Kotlin/Native compatibility, Xcode/SwiftUI target, dependencies, persistence, OAuth, sync, notifications, Calendar/Tasks, tests and build tooling. Perform a display/viewport audit across portrait, landscape, narrow split, medium split, full screen and Stage Manager. Identify information that can be simultaneously exposed on iPad without clutter. Define content-driven layout states and risks. Do not implement later product features. Produce an evidence-based audit, baseline build/test commands, blockers and permanent platform-boundary rule.

## REQUIRED VALIDATION

Inspect the actual repository before deciding. Implement only this phase. Run exact relevant build and automated tests, then runtime validation on iPad Simulator and a real iPad when available. Test failure and recovery states relevant to the phase. Fix failures, rebuild/reinstall/retest, record evidence, update status and permanent editor rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Audit against the CURRENT Android UI (1.0.39 + in-main fixes), not the pre-2026-10-18 plan docs. Record the design delta in `design.md` / `editor-rules.md` as the target the iPad build must match.
- Classify the KMP shared code against the current behavior: the shared layer already carries the current sync/classification/search semantics; the iPad work is presentation + platform boundary, not re-derivation of the design.
- Display/viewport audit must define content-driven layout states (sidebar → list → detail → inspector) across portrait, landscape, narrow split, medium split, full screen, Slide Over, and Stage Manager — never device-name checks.
- Verify the iPad target can consume: the logo pipeline (bundled → asset catalog → favicon → letter), the fixed-denominator progress model, the cursor-resume sync model, and the per-second-throttle pacing model.
- Do not re-derive the design from the old plan docs. Where the old plan and `design.md` (2026-10-18) disagree, `design.md` wins.
