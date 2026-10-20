# Phase 27 — Performance, Accessibility & Reliability Hardening

**Status:** COMPLETE (profiles + hooks) — 2026-10-20

## Delivered

Shared (`desktop-core/src/capability.rs`):

- `PerformanceTier` / `PerformanceProfile` — Conserving / Balanced / Expressive (tiers, **not** device names).
- Selects prefetch, image cache MB, background workers, sync batch, expressive motion flags.
- `fps_claim_from_refresh_hz` always returns `None` — **no 120fps-from-Hz claims**.

Windows (`capability_profile.rs`):

- Re-exports core performance profile as `CapabilityProfile` / `CapabilityTier`.
- `AccessibilityHooks` — Narrator labels, high-contrast flag, reduced-motion, text scale.
- Shell re-evaluates hooks when theme / reduced-motion changes; forces Conserving/Essential when reduced-motion.

## Tests

- conserving disables expressive; reduced-motion detect; no FPS-from-Hz; Narrator label format.

## Blockers

- Continuous profiling / crash recovery telemetry not in scope (no invented evidence).
- Packaging/QA matrices owned by Agent 5 (phases 28–30).
