# Phase 11 — iOS Offline, Background Refresh & Notifications

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is **iOS / iPhone only**. All iOS-specific background, notification, and UI artifacts belong under `iOS/`. No sibling platforms.

## Mission

Make the iPhone application resilient across offline periods, suspension, termination, and notification delivery.

## Scope

- cached mailbox state
- offline search/read behavior where available
- stale indicators
- reconnect/reconcile
- BGTaskScheduler/background refresh where justified
- notification categories/actions
- notification privacy
- duplicate suppression
- deep links
- authentication-required notifications
- cancellation and retry
- battery-aware scheduling

Do not promise exact background execution times.

Notifications must not reveal sensitive email content on a locked screen beyond the user's configured privacy policy.

## Validation

Test airplane mode, flaky network, app suspension, force termination, reboot where practical, delayed background execution, duplicate notifications, notification permission denial, and stale cache.

**STOP AFTER PHASE 11.**

## 2026-10-18 — Current-design update

- Background workers carry an explicit account id and never read the active UI account (protects against races when switching accounts). Periodic intervals clamped to the platform minimum; lifecycle triggers apply a 15-minute staleness threshold (no aggressive polling).
- Offline state is cleanly distinguished from authentication failure or API errors — never show "Reconnect" when simply offline. Last-sync timestamps are truthful, no fake precision.
- iOS: respect BGAppRefresh / background-task scheduling constraints. Never claim a background task will run continuously or at an exact cadence iOS does not guarantee. Notifications are honest (no fake/bait content).
