# Phase 18 — iOS Final Production Hardening & Release

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. All final iOS artifacts, reports, screenshots, release checks, and implementation changes belong under `iOS/`. Never create or modify iPadOS, Android tablet, macOS, Windows, or Linux work.

## Mission

Perform the final production-readiness audit of the iPhone application and make an evidence-based release decision.

## Final audit

Recheck every completed iOS phase and all iOS master documents:

- architecture and KMP/iOS boundary
- Gmail source-of-truth behavior
- sync, retry, recovery, and account isolation
- OAuth/scopes/token security
- unified inbox and receiving-account identity
- categories/company filtering
- search/indexing
- classification/priority/Action Required
- rules/corrections
- temporal/conversation intelligence
- Action Engine
- Calendar/Tasks
- offline/background behavior
- notifications
- AI boundary and fallback
- automation safety
- privacy/security/Keychain/entitlements
- accessibility/Dynamic Type/VoiceOver/Reduce Motion
- dark mode and responsive iPhone UI
- performance/memory/battery/thermal behavior
- large datasets
- migrations/corruption/recovery
- release archive/signing
- App Store metadata/data declarations
- production OAuth configuration
- dependency/security/secret scan
- debug/test bypass removal
- repository hygiene

## Critical smoke path

Run a release build through:

launch → OAuth → sync → All Inbox → open message → search → classification/company filter → star → Action Required → account switch → offline → reconnect.

Also test at least one permission denial, one authentication failure/recovery path, one malformed email/HTML case, and one account-isolation attack.

## Release decision

Declare **RELEASE READY** only when every blocking gate has evidence.

Otherwise declare **RELEASE BLOCKED** and record each blocker, severity, evidence, and remediation required.

Do not claim App Store approval, Google OAuth verification, legal compliance, or policy acceptance without actual evidence. Do not publish automatically.

## Completion protocol

Create the final iOS release-readiness report under `iOS/`. Update permanent editor rules only for genuinely discovered permanent constraints. Verify clean Git state/repository hygiene and ensure no secrets are present.

**STOP AFTER PHASE 18.**

## 2026-10-18 — Current-design update

- Final audit extends the checklist with the current-design items: company logos in EVERY row area (Mail, Categories, import rows); category groups (one-cavity, vertical centring, semantic accents); scrollbar behavior; import flow (arc-not-progress, ring-only progress, progress never backwards, 99% cap, Done gating, error chrome); throttle handling (pacing, Retry-After, no silent skips); no fabricated counts; no app emojis; dark-mode charcoal (never pure black); HTML rendering (entity decoding, no per-image lines, images never fetched); pull-to-refresh finger-only; tab-switch no-reload; Settings footer rule; drawer covering top section.
- Critical smoke path unchanged, plus: one company-logo case, one throttle case, one malformed-HTML case, one permission-denial case.
- Declare RELEASE READY only when every blocking gate has evidence. Otherwise RELEASE BLOCKED with blocker, severity, evidence, remediation. Do not claim App Store approval, Google OAuth verification, legal compliance, or policy acceptance without actual evidence.
