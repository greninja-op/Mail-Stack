# PHASE 00 — Architecture, Shared Rust Core & Linux Desktop Audit

> **LINUX ISOLATION — FIRST RULE:** This phase is Linux-specific. Do not modify macOS, Windows, Android, AndroidTablet, iOS or iPadOS platform code. Shared desktop Rust-core changes are permitted only when required by an explicitly shared contract and must preserve all existing platform contracts.

## Mission
Audit the existing repository and execution-plan architecture. Define the Linux boundary and verify the reusable Rust desktop core contract with macOS/Windows. Record toolchain, workspace, packaging and dependency constraints before implementation.

## Architecture Contract
- Linux is Rust-first.
- Reuse the shared desktop Rust core with macOS and Windows; do not duplicate business logic.
- Keep Linux UI, lifecycle, windowing, notifications, secret storage, filesystem/OS adapters and packaging behind explicit platform boundaries.
- Preserve the established Mail Organizer product behavior and design language.
- Gmail remains the cloud source of truth; local state is Mail Organizer state and rebuildable indexes.
- Email is untrusted input and cannot authorize external actions.

## Scope
Implement only the work required by this phase. Inspect the actual repository before editing. Do not pre-implement later phases merely because dependencies are convenient.

## Verification
1. Inspect affected source, tests, build configuration and existing behavior.
2. Implement the phase with focused unit/integration tests.
3. Build the relevant Linux target/profile.
4. Launch the real application and verify runtime behavior where the environment permits.
5. Inspect logs, persistence and failure states relevant to this phase.
6. Verify accessibility, visual states, keyboard/mouse behavior and performance where applicable.
7. Test error, cancellation, offline and recovery paths relevant to the phase.
8. Fix failures and rebuild/retest until the phase is genuinely verified.
9. Never claim a test or runtime check that was not actually executed.

## Security & Privacy
- Never commit OAuth secrets, tokens, credentials or private test data.
- Redact secrets from logs and diagnostics.
- Do not execute JavaScript from email HTML.
- Do not automatically follow links or perform external side effects from email content.
- Use least privilege and explicit confirmation for external writes.

## Documentation & Rules
After verification, update phase status and relevant documentation. Add only genuinely permanent new rules to `execution plan/Linux/editor-rules.md`; do not delete or regenerate unrelated rules.

## Completion
The phase is complete only when implementation, tests, runtime verification where applicable, documentation and regression checks are complete.

**STOP AFTER PHASE 00. Do not continue to the next phase.**

## 2026-10-18 — Current-design update

- Audit against the CURRENT Android UI (1.0.39 + in-main fixes), not the pre-2026-10-18 plan docs. Record the design delta in `design.md` / `editor-rules.md` as the target the Linux build must match.
- Classify the shared Rust desktop core against the current behavior: the shared layer must carry the current sync/classification/search semantics (fixed-denominator progress, cursor-resume sync, per-second-throttle pacing, logo-pipeline domain detection); the Linux work is presentation + platform boundary, not re-derivation of the design.
- Verify the Linux target can consume: the logo pipeline (bundled → asset catalog → favicon → letter), the fixed-denominator progress model, the cursor-resume sync model, and the per-second-throttle pacing model.
- Do not re-derive the design from the old plan docs. Where the old plan and `design.md` (2026-10-18) disagree, `design.md` wins.
