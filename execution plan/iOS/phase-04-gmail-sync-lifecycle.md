# Phase 4 — Gmail Synchronization & iOS Lifecycle

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target is **iOS / iPhone only**. iOS-specific sync lifecycle code/config/tests stay under `iOS/`. Do not touch iPadOS, Android tablet, macOS, Windows, or Linux.

## Mission

Connect the established Gmail synchronization engine to iOS while respecting iOS lifecycle and background-execution constraints.

## Scope

- initial account sync
- incremental sync/cursors/history where supported by existing architecture
- pagination
- retry/backoff
- cancellation
- partial failure recovery
- stale/fresh/syncing/offline states
- foreground refresh
- iOS background refresh/task integration where justified
- per-account and aggregate sync state
- honest sync progress
- sync diagnostics without sensitive logging

Use the existing KMP sync engine rather than creating an iOS-specific duplicate.

## UX

Implement the established Gmail → Mail Organizer retrieval concept. Determinate progress only when trustworthy; otherwise indeterminate. Never fake a percentage.

## Validation

Test first sync, incremental sync, interruption, process termination, expired auth, offline start, network recovery, duplicate prevention, account isolation, and background scheduling behavior. Do not claim exact background execution cadence.

**STOP AFTER PHASE 4.**

## 2026-10-18 — Current-design update

- **Daily quota is effectively never the problem** (one 500-message first sync ≈ 2,682 units vs 1,000,000,000 daily). The real limit is the **per-second throttle (250 units/user/sec)**.
- Pace requests (token bucket, ~10 req/sec) + honor Gmail's `Retry-After` header. **Never silently skip a throttled per-message GET** — retry dropped GETs; a skipped message is silently missing forever.
- Do NOT fire retry LIST calls while Gmail is throttling (they extend the throttle).
- Join in-flight syncs per account, never cancel/restart. Cursor resume (see Phase 2).
- Progress from the real sync only, never backwards: total fetched ONCE (Gmail profile `messagesTotal`) before the loop, bounded by the per-run max; caps at 99% while running; Done = 100%.
- Background refresh + scheduler call the same sync entry point (staleness > 15 min). Spam pass runs once per first sync.
- Classify failures honestly: per-second throttle ("wait a minute, tap once") vs daily quota ("resets at midnight Pacific, try tomorrow") vs permission vs offline. Never say "Reconnect" when offline.
