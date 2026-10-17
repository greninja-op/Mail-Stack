# Phase 28 — Comprehensive Functional & Integration QA

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

QA the entire iPad product through Phase 27. Build a traceable matrix covering install/upgrade, launch/relaunch, OAuth, sync, mailbox, categories, companies, search, priority, rules, temporal/conversation, Action Engine, Calendar/Tasks, multi-account, offline/background, notifications, AI and automation. Use synthetic and realistic datasets. Classify P0/P1/P2/P3. Every defect fix is retested. Never fabricate evidence.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- QA the entire product through Phase 27. Traceable matrix: install/upgrade, launch/relaunch, OAuth, sync, mailbox, categories, companies, search, priority, rules, temporal/conversation, Action Engine, Calendar/Tasks, multi-account, offline/background, notifications, AI, automation. Use synthetic + realistic datasets. Classify P0/P1/P2/P3. Every defect fix is retested. Never fabricate evidence.
- **Never claim a test, device run, screenshot, log, or validation that was not actually performed.** Build success alone is never sufficient.
- Cover: light/dark mode, Dynamic Type sizes, RTL-safe layout, long subjects, large threads, missing sender/avatar data, network interruptions, malformed email/HTML cases, permission denial, authentication failure/recovery, account-isolation attack, throttle simulation (per-second throttle vs daily quota), logo-pipeline cases (company vs personal vs unknown domain), category-group rendering (one-cavity, vertical centring, accent colors), import-flow cases (progress never backwards, 99% cap, Done gating, error chrome), all window modes.
- Screenshot review on every major UI change. Jank/frame-time inspection where practical. Regression review of existing screens.
