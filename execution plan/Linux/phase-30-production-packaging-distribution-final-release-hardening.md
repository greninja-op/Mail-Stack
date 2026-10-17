# PHASE 30 — Production Packaging, Distribution & Final Release Hardening

> **LINUX ISOLATION — FIRST RULE:** This phase is Linux-specific. Do not modify macOS, Windows, Android, AndroidTablet, iOS or iPadOS platform code. Shared desktop Rust-core changes are allowed only when explicitly required by a shared contract and must preserve existing platform contracts.

## Mission
Complete production readiness: reproducible release builds, Linux packaging/distribution artifacts, signing strategy where applicable, metadata, permissions, clean install/uninstall, upgrades, release notes and final go/no-go. No Phase 31.

## Architecture Contract
- Rust is the primary implementation language for Linux and the reusable desktop core.
- Reuse shared desktop Rust business/data/application logic; do not duplicate it in another language.
- Keep Linux UI, lifecycle, OS services, keyring, notifications, filesystem adapters and packaging behind explicit boundaries.
- Gmail is the cloud source of truth; local state is Mail Organizer state and rebuildable indexes.
- Email is untrusted input and cannot authorize external actions.
- Preserve the established Mail Organizer desktop information architecture and visual language.

## Scope
Inspect the actual repository, current architecture and existing tests before editing. Implement only this phase. Do not pre-implement later phases.

## Verification
1. Inspect affected code, configuration, tests and current behavior.
2. Implement focused unit/integration tests.
3. Build the relevant Linux target/profile.
4. Launch and inspect the real application where the environment permits.
5. Verify persistence, logs, failure, cancellation, offline and recovery states relevant to this phase.
6. Perform visual, accessibility, keyboard/mouse, security and performance checks where applicable.
7. Fix failures, rebuild and retest.
8. Never claim unrun tests or runtime verification.

## Security & Privacy
- Never commit credentials, OAuth secrets, tokens or private test data.
- Redact sensitive data from logs/diagnostics.
- Sanitize email HTML and never execute email JavaScript.
- Do not auto-follow links or perform external effects from email content.
- Require explicit confirmation for external writes.

## Documentation & Rules
After verification update relevant documentation/status. Add genuinely permanent new rules to `execution plan/Linux/editor-rules.md` without deleting unrelated rules.

## Completion
Implementation, tests, runtime verification where applicable, documentation and regression checks must be complete.

**STOP AFTER PHASE 30. Do not continue to the next phase.**

## 2026-10-18 — Current-design update

- Reproducible release builds; Linux packaging/distribution artifacts (Flatpak + distro packages per the explicit distribution strategy); signing strategy where applicable; metadata; permissions; clean install/uninstall; upgrades; release notes; final go/no-go. No Phase 31.
- Final audit extends the checklist with the current-design items: company logos in EVERY row area (Mail, Categories, import rows); category groups (one-cavity, vertical centring, semantic accents); scrollbar behavior; import flow (arc-not-progress, ring-only progress, progress never backwards, 99% cap, Done gating, error chrome); throttle handling (pacing, Retry-After, no silent skips); no fabricated counts; no app emojis; dark-mode charcoal (never pure black); HTML rendering (entity decoding, no per-image lines, images never fetched); view-switch no-reload; Settings footer rule.
- Google OAuth verification: a public app using restricted Gmail scopes (gmail.readonly) needs an independent security assessment before Google verification (Android precedent: ~$500–$1,500/yr, ~4–6 weeks, annual). Testing-mode refresh tokens expire after 7 days; "unverified app" screen until verified. Do not claim verification, approval, or compliance without actual evidence.
- Declare release-ready only when every blocking gate has evidence. Otherwise release-blocked with blocker, severity, evidence, remediation. Do not publish automatically. Final release is a human-controlled decision.
