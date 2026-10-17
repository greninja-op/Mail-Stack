# Phase 05 — Material 3 Large-Screen Visual System

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Establish shared semantic design tokens and tablet presentation tokens: typography, spacing, surfaces, colors, shapes, icon semantics, motion and states. Use Material 3/current large-screen guidance without cloning phone UI. Validate light/dark, large text, contrast, reduced motion and dense/wide layouts. Do not duplicate business constants.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 05.**

## 2026-10-18 — Current-design update

- Tokens come from `design.md` (2026-10-18): brand violet `#5238E8`/`#B9A8FF`, dark-mode charcoal `#131120` never pure black, typography scale, 28dp card radius, spacing, motion tiers, semantic category accents (amber/orange/sky/royal-blue/brand-violet). Same tokens as the phone app — no per-screen magic values, no second palette.
- Mail rows are flat rows on the background (no card/border/shadow). Cards are reserved for grouped content (category groups, settings groups) with clip-before-clickable.
- Validate light/dark, large text, contrast, reduced motion, and dense/wide layouts. Do not duplicate business constants.
