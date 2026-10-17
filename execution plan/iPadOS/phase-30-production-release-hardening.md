# Phase 30 — Production Signing, App Store Preparation & Final Release Hardening

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Final gate. Verify all phase evidence and master documents. Audit bundle identity, signing/provisioning, entitlements, OAuth release identity/scopes, privacy/data declarations, permissions, App Store assets, version/build numbers, release configuration, dependency/security/secret scans, debug/test bypass removal, archive reproducibility and repository hygiene. Build a clean production archive and release smoke test: launch→OAuth→sync→multi-column All Inbox→detail→search→company filter→star→Action Required→account switch→offline→reconnect. Verify multiple window modes and accessibility. Decide RELEASE READY only with evidence; otherwise RELEASE BLOCKED with severity/evidence/remediation. Do not publish or claim App Store/OAuth approval without evidence. STOP AFTER PHASE 30.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Final gate. Verify all phase evidence and master documents. Audit bundle identity, signing/provisioning, entitlements, OAuth release identity/scopes, privacy/data declarations, permissions, App Store assets, version/build numbers, release configuration, dependency/security/secret scans, debug/test bypass removal, archive reproducibility, repository hygiene.
- Final audit extends the checklist with the current-design items: company logos in EVERY row area (list, categories, detail, import rows); category groups (one-cavity, vertical centring, semantic accents); scrollbar behavior; import flow (arc-not-progress, ring-only progress, progress never backwards, 99% cap, Done gating, error chrome); throttle handling (pacing, Retry-After, no silent skips); no fabricated counts; no app emojis; dark-mode charcoal (never pure black); HTML rendering (entity decoding, no per-image lines, images never fetched); pull-to-refresh finger-only; selection/column changes never reload; Settings footer rule; sidebar destinations.
- Clean production archive + release smoke test: launch → OAuth → sync → multi-column All Inbox → detail → search → company filter → star → Action Required → account switch → offline → reconnect. Verify multiple window modes and accessibility. Decide RELEASE READY only with evidence; otherwise RELEASE BLOCKED with severity/evidence/remediation.
- Google OAuth verification: a public app using restricted Gmail scopes (gmail.readonly) needs a CASA-style independent security assessment before Google verification (Android precedent: ~$500–$1,500/yr, ~4–6 weeks, annual). Testing-mode refresh tokens expire after 7 days. Do not publish or claim App Store/OAuth approval without evidence. STOP AFTER PHASE 30.
