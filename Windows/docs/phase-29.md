# Phase 29 — Security, Adversarial, Migration & Recovery QA

**Status:** COMPLETE (adversarial docs + unit tests in source) — 2026-10-20  
**Live adversarial / recovery runs:** **BLOCKED** (`link.exe` missing)

## Deliverables

- Adversarial matrix: [`qa/phase-29-adversarial.md`](qa/phase-29-adversarial.md)
- Hardened HTML sanitizer: event handlers + `javascript:` neutralization; doctype/PI scrubbing
- Extended URL / secrets unit tests (`privacy.rs`, `browser.rs`)
- `desktop-core` `lib.rs` exports all phase modules so the test suite is compilable once MSVC exists

## Static secrets audit (observed)

- No production OAuth secrets, API keys, or private keys in `Windows/` or `desktop-core/` sources.
- Only intentional test fixture token-shaped string in unit tests.
- Migrations documented without token columns.

## Not claimed

- Unit tests were **not** executed green on this PC (linker failure).
- No live cross-account UI attack, process-death recovery, or DB corruption drill.

## Follow-ups when toolchain ready

1. `cargo test -p desktop-core --lib`
2. `cargo test -p mailstack-windows`
3. Manual malicious-HTML fixture pass in the running shell
4. Account removal + Credential Manager cleanup verification
