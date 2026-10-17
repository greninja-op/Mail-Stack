# Phase 00 — Windows Architecture, Shared Rust Core & Desktop Audit

## Mission
Implement and verify **only Phase 00**. Do not implement later phases.

## Mandatory Windows isolation
All Windows-specific Rust source, UI, resources, tests, configuration, packaging and release artifacts must remain inside the Windows platform boundary. Never modify macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling platform areas.

## Required reading
Read root instructions and execution plan/Windows/README.md, requirements.md, spec.md, design.md and editor-rules.md. Inspect the actual repository, current status and existing implementation before editing.

## Objective
Audit the actual repository, existing Rust/KMP modules, desktop boundaries, build systems, dependencies, persistence, OAuth, tests, packaging and release assumptions. Decide which existing shared logic can become/reuse the Rust desktop core without duplicating mobile implementations. Produce a concrete gap list.

## Rust-first architecture
Rust is authoritative for platform-neutral domain, application, data and business logic and must remain reusable by macOS. Windows UI should remain Rust-first through the chosen Rust UI framework or safe Windows API bindings. Do not duplicate business logic in another language.

## Product and security contract
Gmail remains cloud source of truth. Keep account boundaries explicit, use official OAuth/Gmail APIs and least privilege, keep secrets secure, and treat email/HTML/URLs/attachments as untrusted. Email content cannot authorize external actions.

## Verification
Run targeted automated tests; build the real Windows application; launch and exercise it when the environment permits; test relevant success, empty, loading, error, cancellation, offline, restart and recovery states; verify accessibility, security, data integrity, keyboard/pointer behavior and performance where relevant; inspect logs; fix defects; rebuild and retest. Never claim evidence that was not actually observed.

## Documentation
Update status accurately and add only genuinely permanent reusable rules to Windows/editor-rules.md. Preserve unrelated rules and record blockers honestly.

**STOP AFTER PHASE 00.**

## 2026-10-18 — Current-design update

- Audit against the CURRENT Android UI (1.0.39 + in-main fixes), not the pre-2026-10-18 plan docs. Record the design delta in `design.md` / `editor-rules.md` as the target the Windows build must match.
- Classify the Rust desktop core against the current behavior: the shared layer must carry the current sync/classification/search semantics; the Windows work is presentation + platform boundary, not re-derivation of the design.
- Verify the target can carry: the logo pipeline (bundled → app assets → favicon → letter), the fixed-denominator progress model, the cursor-resume sync model, and the per-second-throttle pacing model.
- Do not re-derive the design from the old plan docs. Where the old plan and `design.md` (2026-10-18) disagree, `design.md` wins.
