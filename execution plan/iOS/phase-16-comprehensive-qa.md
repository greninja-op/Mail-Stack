# Phase 16 — iOS Comprehensive QA & Adversarial Testing

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. All QA artifacts for this phase remain under `iOS/`. Do not implement iPadOS, Android tablet, macOS, Windows, or Linux work.

## Mission

Perform comprehensive end-to-end QA of the real iPhone application through Phase 15. This phase is validation and defect fixing, not feature expansion.

## Quality gates

Run evidence-based checks for:

- clean build and dependency integrity
- static analysis and secret scanning
- fresh install and upgrade
- launch/relaunch/process termination/recovery
- Google OAuth and token refresh
- single-account and multi-account isolation
- cross-account attack attempts
- Gmail sync integrity, pagination, interruption, and recovery
- email parsing and hostile HTML/URL inputs
- classification/company/priority/Action Required
- search/index integrity and large datasets
- rules/corrections
- temporal/conversation intelligence
- Action Engine
- Calendar/Tasks permissions and failure
- offline/reconnect/background behavior
- notification privacy and duplication
- AI-disabled, unavailable, malformed-output, and prompt-injection cases
- automation duplication, retry, loop, stale-schedule, permission, and account-removal cases
- privacy/security/Keychain/entitlements
- VoiceOver, Dynamic Type, Reduce Motion, dark mode
- performance/memory/storage/battery
- migration/corruption/recovery
- release configuration

Classify defects P0/P1/P2/P3. Every defect fix must be retested. Never report tests that were not actually run and never fabricate screenshots, device results, logs, or performance measurements.

Verify on iPhone Simulator and a real iPhone when available.

**STOP AFTER PHASE 16.**

## 2026-10-18 — Current-design update

- Never claim a test, device run, screenshot, log, or validation that was not actually performed. Build success alone is never sufficient.
- Cover: light/dark mode, Dynamic Type sizes, RTL-safe layout, long subjects, large threads, missing sender/avatar data, network interruptions, malformed email/HTML cases, permission denial, authentication failure/recovery, account-isolation attack, throttle simulation (per-second throttle vs daily quota), logo-pipeline cases (company vs personal vs unknown domain), category-group rendering (one-cavity, vertical centring, accent colors), import-flow cases (progress never backwards, 99% cap, Done gating, error chrome).
- Screenshot review on every major UI change. Jank/frame-time inspection where practical. Regression review of existing screens.
