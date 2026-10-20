# Phase 17 — Priority, Action Required & Explainability

**Status:** COMPLETE (2026-10-20)

## Delivered
- `desktop-core/src/priority.rs` — priority + AR assessments with explanations
- `MailListBadgeFlags` has **no** Action Required chip flag (`supports_action_required_chip() == false`)
- Conversation viewer shows “Why this priority?”

## Permanent rules
- No AR chip/filter in the Mail list.
- Priority badges carry explainability from core — UI does not invent reasons.
