# Phase 24 — Optional AI Fallback

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Expose the existing optional AI architecture. Deterministic-first routing, confidence thresholds, structured output validation, prompt-injection defense, minimal context, sensitive-data controls, provider privacy disclosure, enable/disable, failure/offline fallback, usage visibility and provenance. AI may assist intelligence but can never authorize Gmail/Calendar/Tasks/automation/external actions. Test disabled provider, unavailable provider, malformed output, adversarial email, timeout, offline and deterministic fallback.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Optional AI architecture, deterministic-first routing. AI is an optional secondary fallback, never the foundation. Deterministic local intelligence remains authoritative. AI disabled by default with explicit user opt-in. AI is never required for core mailbox access, search, classification, or safety.
- DataMinimizer: strip HTML, MIME headers, recipient lists; truncate bodies to ≤500 characters; sanitize identifiers (emails/tokens). Prompt boundary: anti-injection delimiter shielding.
- Output validator: strict structured JSON schemas, model confidence capped at 0.85, reject executable command verbs (delete/send/drop), and NEVER downgrade deterministic SECURITY classifications. AI output never directly executes actions. AI can never authorize Gmail/Calendar/Tasks/automation/external actions.
- Confidence thresholds, structured output validation, prompt-injection defense, minimal context, sensitive-data controls, provider privacy disclosure, enable/disable, failure/offline fallback, usage visibility, provenance. On-demand thread summarization is explicitly user-initiated with a clear advisory notice ("Advisory summary · May contain errors"). Subtle "AI suggestion" badge pill where a suggestion appears.
- Test disabled provider, unavailable provider, malformed output, adversarial email, timeout, offline, deterministic fallback.
