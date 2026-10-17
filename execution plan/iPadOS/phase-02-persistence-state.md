# Phase 2 — Shared Mobile Contract & Platform Boundary

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific implementation, tests and documents belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Define and enforce the shared mobile contract between Android, iPhone and iPad. Inspect existing common source and identify duplicated globals/models/configuration that must not diverge. Consolidate only genuinely common constants, semantic states, domain models and interfaces into shared source when duplication is proven; do not redesign unrelated systems. Define platform adapters for UI, lifecycle, secure storage, background execution, notifications and OS integrations. Verify iPad consumes common behavior rather than copying it. Add compile/unit checks proving the common contract remains valid for existing platforms.

## REQUIRED VALIDATION

Inspect the actual repository before deciding. Implement only this phase. Run exact relevant build and automated tests, then runtime validation on iPad Simulator and a real iPad when available. Test failure and recovery states relevant to the phase. Fix failures, rebuild/reinstall/retest, record evidence, update status and permanent editor rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- The shared mobile contract carries the CURRENT behavior: account isolation as a first-class DB boundary (FK-enforced), cursor-based sync resume, insertion-order rows, no tokens in DB, FTS failure-safe creation, demo/seeded accounts never sync.
- Consolidate only genuinely common constants, semantic states, domain models, and interfaces into shared source when duplication is proven; do not redesign unrelated systems. iPad consumes common behavior rather than copying it.
- Platform adapters for UI, lifecycle, secure storage, background execution, notifications, and OS integrations — iPad-specific adapters live under `iPadOS/` only.
