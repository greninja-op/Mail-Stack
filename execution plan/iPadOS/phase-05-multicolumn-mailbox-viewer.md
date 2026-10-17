# Phase 5 — Design Tokens, Theming & iPad Visual System

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Establish shared semantic visual tokens without duplicating business constants. Define typography, spacing, colors, surfaces, shapes, icon semantics, motion and state tokens so Android/iPhone/iPad share product meaning while iPad can express its own layout. Implement light/dark and accessibility variants. Evaluate supported Apple visual materials, including Liquid Glass-era public APIs where appropriate, without private APIs or unofficial clones. Validate token consistency across sidebar/list/detail, Dynamic Type, contrast, Reduce Motion and narrow widths. Do not implement mailbox data.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior phase evidence. Implement only this phase. Run targeted unit/integration tests and runtime tests on iPad Simulator and real iPad where available. Exercise success, failure, cancellation and recovery paths appropriate to the phase. Inspect logs and persisted state. Fix, rebuild/reinstall/retest. Record exact evidence. Update status and permanent rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Tokens are the CURRENT system from `design.md`: brand violet `#5238E8` / `#B9A8FF`; dark-mode charcoal `#131120`, never pure black; semantic category accents (amber/orange/sky/royal-blue/brand-violet); typography scale (display 64/57, title 22/17/16, body 16/14, label 14/12); 28pt card radius; motion tiers (Essential/Standard/Expressive/Reduced Motion).
- Mail rows are flat rows on the background (no card/border/shadow). Cards are reserved for grouped content (category groups, settings groups, inspector groups) — solid tonal fill, tap highlight clipped to the rounded shape.
- **No glass UI / Liquid Glass as the design language.** Public Apple visual materials may be used only where they do not change the design language, never via private APIs. The import card's user-supplied frosted host is the only glass exception.
- Validate token consistency across sidebar/list/detail/inspector, Dynamic Type, contrast, Reduce Motion, and narrow widths. No per-screen magic values. Do not implement mailbox data.
