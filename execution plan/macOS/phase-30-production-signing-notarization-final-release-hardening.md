## Mandatory Rust desktop architecture

**Rust is the primary implementation language for this macOS phase.** The reusable Rust desktop core owns platform-neutral business/data/application behavior and must be designed for Windows reuse. Swift/SwiftUI is only the native macOS presentation and OS integration layer; it must not duplicate Rust logic.

# Phase 30 — Production Signing, Notarization & Final Release Hardening

## Mission
Implement and verify only Phase 30. This is a sequential roadmap. Do not implement any later phase.

## Mandatory macOS isolation
Modify only the macOS platform boundary and genuinely shared Rust desktop core/common code required by this phase. Never create, modify, delete, rename, or reorganize iOS, iPadOS, AndroidTablet, Android, Windows, Linux, or other sibling platform areas.

## Required reading and inspection
Read root instructions and execution plan/macOS/README.md, requirements.md, spec.md, design.md, and editor-rules.md. Determine the real current status and inspect the actual repository before editing. Preserve newer repository decisions and do not replace master documents with stale copies.

## Scope
Perform final release hardening: release configuration, bundle identifiers, entitlements, signing, hardened runtime, sandbox permissions, OAuth release configuration, privacy/data disclosures, dependency review, clean release build/archive, notarization and stapling validation where applicable, clean-machine install/smoke test, artifact inspection, versioning, rollback/update documentation, and an explicit RELEASE READY or RELEASE BLOCKED decision. Never publish automatically.

## Engineering contract
Use native macOS/SwiftUI patterns and the shared Rust desktop core core where appropriate. Preserve Gmail as source of truth, explicit account boundaries, least-privilege OAuth, secure credentials, untrusted-email handling, deterministic/local/explainable behavior before AI, and confirmation/idempotency for consequential external actions.

## Verification gate
Run targeted automated tests and relevant integration tests. Build the real macOS application with the supported toolchain. Launch and exercise it when the environment permits. Test realistic success, empty, loading, error, cancellation, offline, restart, sleep/wake, recovery and partial-failure states relevant to this phase. Check accessibility, keyboard/pointer behavior, security, data integrity, logs, memory/performance and release configuration as applicable. Fix defects, rebuild/relaunch/reinstall where appropriate, and retest.

Never claim tests, screenshots, runtime results, performance metrics, signing, notarization, or device/OS checks that were not actually performed. Record blockers honestly.

## Documentation
Update phase status accurately. Add only genuinely permanent reusable rules to macOS editor-rules.md and never delete unrelated rules.

**STOP AFTER PHASE 30.** The macOS roadmap ends here; do not create Phase 31.

## 2026-10-18 — Current-design update

- Final audit extends the checklist with the current-design items: company logos in EVERY row area (Mail, Categories, import rows); category groups (one-cavity, vertical centring, semantic accents); scrollbar behavior; import flow (arc-not-progress, ring-only progress, progress never backwards, 99% cap, Done gating, error chrome); throttle handling (pacing, Retry-After, no silent skips); no fabricated counts; no app emojis; dark-mode charcoal (never pure black); HTML rendering (entity decoding, no per-image lines, images never fetched); refresh explicit-only; destination-switch no-reload; Settings footer rule; sidebar stability.
- Release configuration: bundle identifiers, entitlements, signing, hardened runtime, sandbox permissions, OAuth release configuration, privacy/data disclosures, dependency review, clean release build/archive, notarization + stapling validation where applicable, clean-machine install/smoke test, artifact inspection, versioning, rollback/update documentation.
- Google OAuth verification: a public app using restricted Gmail scopes (gmail.readonly) needs an independent security assessment before Google verification (Android precedent: ~$500–$1,500/yr, ~4–6 weeks, annual). Testing-mode refresh tokens expire after 7 days. Do not claim verification, approval, or compliance without actual evidence.
- Declare RELEASE READY only when every blocking gate has evidence. Otherwise RELEASE BLOCKED with blocker, severity, evidence, remediation. Do not publish automatically.
