# Phase 20 — Action Cards

**Status:** COMPLETE — 2026-10-20

COMPLETE (desktop-core). ctions.rs: ActionEngine propose/confirm; CalendarService + TasksService traits; disconnected → NotConnected (no fake success).

Shared crate: `desktop-core`. Windows UI adapters consume these contracts; do not reimplement precedence/sync honesty in presentation code.
