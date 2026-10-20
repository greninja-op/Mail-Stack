# Phase 24 — Optional AI Fallback

**Status:** COMPLETE (desktop-core) — 2026-10-20

## Delivered

Shared (`desktop-core/src/ai.rs`):

- `AiSettings` / `AiRouter` — **disabled by default**; route fails closed when off.
- `DataMinimizer` / `minimize_for_ai` — strip HTML/MIME headers; truncate body ≤500 chars; sender domain only.
- `wrap_prompt_boundary` — anti-injection delimiter shield.
- `validate_ai_output` — confidence **capped at 0.85**; rejects executable command verbs; **never downgrades SECURITY** (fail closed when deterministic SECURITY is set).
- `AiRouter::can_authorize_actions()` always returns **false** — AI cannot authorize Gmail/Calendar/Tasks/automation/external actions.
- Advisory copy: `summary_advisory_notice()`, `ai_suggestion_badge()`.

## Tests

- disabled-by-default, minimize truncate, verb reject, confidence cap, SECURITY shield, injection wrap.

## Blockers

- No live provider wired on Windows yet (optional by design).
- Host MSVC linker required to execute `cargo test` on this PC.
