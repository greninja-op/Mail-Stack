# Phase 16 — Search

**Status:** COMPLETE — 2026-10-20

COMPLETE (desktop-core). search.rs: structured SearchQuery DSL, FTS escape builder, 300ms SearchDebouncer.

Shared crate: `desktop-core`. Windows UI adapters consume these contracts; do not reimplement precedence/sync honesty in presentation code.
