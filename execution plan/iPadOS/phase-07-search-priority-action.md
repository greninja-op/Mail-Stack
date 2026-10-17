# Phase 7 — Local Persistence & State Hydration

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Connect the existing common persistence/state layer to iPadOS. Define startup hydration, account-scoped caches, schema/migration handling, state invalidation and corruption recovery. Secrets must remain outside ordinary DB state. Verify restart, process termination, migration, corrupted DB, partial writes, account removal and reinstall semantics. Test that the iPad workspace can restore list/detail/navigation state without confusing persisted data with live Gmail truth. Do not implement Gmail sync yet.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior phase evidence. Implement only this phase. Run targeted unit/integration tests and runtime tests on iPad Simulator and real iPad where available. Exercise success, failure, cancellation and recovery paths appropriate to the phase. Inspect logs and persisted state. Fix, rebuild/reinstall/retest. Record exact evidence. Update status and permanent rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Connect the existing common persistence/state layer. Startup hydration, account-scoped caches, schema/migration handling, state invalidation, corruption recovery. Secrets outside ordinary DB state.
- Account isolation is a first-class DB boundary (FK-enforced). Sync cursor saved only after a page fully persists; retries resume from the saved cursor. Rows surface the most recently INSERTED mails (insertion order).
- FTS/index creation is failure-safe (a missing FTS module must never abort DB creation). Table-existence guards on all index cleanup paths. Demo/seeded accounts never sync.
- Verify restart, process termination, migration, corrupted DB, partial writes, account removal, and reinstall semantics. The workspace must restore list/detail/navigation/selection state without confusing persisted data with live Gmail truth. Do not implement Gmail sync yet.
