# Phase 2 — iOS Local Persistence & State Integration

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is **iOS / iPhone only**. All iOS-specific artifacts belong under `iOS/`. Do not touch iPadOS, Android tablet, macOS, Windows, or Linux. Do not duplicate KMP persistence logic in Swift.

## Mission

Connect the existing KMP local persistence/state architecture to the iOS application safely.

## Scope

- inspect and reuse the existing KMP database/state layer
- iOS initialization and lifecycle integration
- account-scoped local state
- migrations and schema version handling
- cache/state hydration
- corruption/failure handling
- secure separation of secrets from ordinary state
- test data isolation
- clean uninstall/reinstall semantics

The Gmail cloud remains authoritative. Local state must be rebuildable where possible.

## Validation

Test fresh install, restart, process termination, migration, corrupted/invalid local state handling, account separation, and reinstall. Verify no tokens or sensitive credentials are placed in ordinary database records or logs.

Do not implement the Gmail sync engine yet.

**STOP AFTER PHASE 2.**

## 2026-10-18 — Current-design update

- Account isolation is a first-class DB boundary (FK-enforced) — never break it.
- Sync state: cursor-based resume (cursor saved only after a page fully persists); retries resume from the saved cursor. Rows surface the most recently INSERTED mails (insertion order, not modification order).
- No tokens/passwords in the DB by schema design. FTS/index creation is failure-safe (a missing FTS module must never abort DB creation). Table-existence guards on all index cleanup paths.
- Demo/seeded accounts never sync.
