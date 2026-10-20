# Phase 18 — Rules & User Corrections

**Status:** COMPLETE (2026-10-20)

## Delivered
- `desktop-core/src/rules.rs` — structured rules (never executable code)
- Precedence: **Correction > Rule > Deterministic > AI > Unknown**
- Undo = delete correction row (engines restore base; never stored twice)
- Empty-condition rules never match; conflict detection by condition signature

## Permanent rules
- Corrections win over rules. Undo deletes the correction.
- Rules are data — never arbitrary code execution.
