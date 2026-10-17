# Phase 17 — Priority, Action Required & Explainability

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Expose common priority and Action Required intelligence in the iPad workspace. Show explainable indicators in list/detail without overwhelming dense layouts. Ensure deterministic precedence, account scope, stale/unknown states and user corrections can be represented. Test borderline messages, missing metadata, deadlines/meeting signals not yet available, conflicting signals, sorting/filtering interactions and accessibility labels. Do not create a second priority engine.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior evidence. Implement only this phase. Run targeted unit/integration tests plus iPad Simulator and real iPad validation where available. Test multiple viewport modes, accessibility and relevant failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Priority + Action Required intelligence in the workspace. Explainable indicators in list/detail/inspector without overwhelming dense layouts. Deterministic precedence, account scope, stale/unknown states, user corrections representable.
- Priority badges carry "why this priority?" explanations; classification carries "Why this category?" explanations.
- **No "Action Required" chip/filter in the Mail list** (removed on Android). Action Required remains available as a destination/section where defined, but not as a Mail-list chip.
- Test borderline messages, missing metadata, deadlines/meeting signals not yet available, conflicting signals, sorting/filtering interactions, and accessibility labels. Do not create a second priority engine.
