# Phase 16 — Search & Local Index

**Status:** COMPLETE (2026-10-20)

## Delivered
- `desktop-core/src/search.rs` — `SearchQuery` DSL, `escape_fts_term`, `build_fts_match`, 300ms `SearchDebouncer`
- Windows search overlay rebuilds structured query from text; shows escaped FTS preview
- UI never builds SQL

## Permanent rules
- Structured query all the way down; every FTS term is escaped.
- Debounce = 300ms quiet after last keystroke.
