# Phase 10 — Gmail Data Access & API Adapter

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Build the iPad-facing Gmail API/data adapter boundary on top of existing common architecture. Normalize Gmail message/thread/label/profile/history/page tokens into shared domain contracts. Handle pagination, rate limits, transient errors, authorization failures and malformed responses. Never expose raw provider objects throughout SwiftUI. Add deterministic mapping tests for missing fields, malformed headers, duplicate IDs, deleted messages and partial responses. Do not implement the full sync workflow yet.

## ACCEPTANCE / VALIDATION

Inspect the actual repository and prior evidence. Implement only this phase. Run targeted automated tests, then iPad Simulator and real iPad validation where available. Test multiple viewport states relevant to the feature and exercise failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**

## 2026-10-18 — Current-design update

- Gmail API/data adapter on the common architecture. Normalize message/thread/label/profile/history/page tokens into shared domain contracts. Handle pagination, rate limits, transient errors, authorization failures, and malformed responses. Never expose raw provider objects throughout SwiftUI.
- **Rate-limit handling (current):** daily quota is effectively never the problem (≈2,682 units per 500-message sync vs 1,000,000,000 daily). The real limit is the **per-second throttle (250 units/user/sec)**. Pace requests (token bucket, ~10 req/sec) + honor `Retry-After`. Classify failures honestly (throttle vs daily quota vs permission vs offline) — never "Reconnect" when offline. Do NOT fire retry LIST calls while throttling.
- Add deterministic mapping tests for missing fields, malformed headers, duplicate IDs, deleted messages, and partial responses. Do not implement the full sync workflow yet.
