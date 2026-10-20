# Phase 10 — Gmail API Adapter

**Status:** COMPLETE — 2026-10-20

COMPLETE (desktop-core). gmail.rs: models, GmailApi trait + MockGmailApi, full-body error classify (throttle / dailyLimit / insufficientScopes), never-skip throttle→pacer hook.

Shared crate: `desktop-core`. Windows UI adapters consume these contracts; do not reimplement precedence/sync honesty in presentation code.
