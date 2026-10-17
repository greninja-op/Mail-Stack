# Phase 7 — iOS Search, Priority & Action Required

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is **iOS / iPhone only**. All platform-specific work belongs under `iOS/`; no sibling platforms.

## Mission

Expose fast local search and explainable priority/action intelligence on iPhone.

## Scope

- local indexed search
- sender/subject/body/thread/company/domain/category/priority/action/Gmail-label fields as supported
- search suggestions/recent searches without leaking sensitive data
- pagination and cancellation
- empty/no-result/error/offline states
- priority indicators
- Action Required indicators and explanations
- safe ranking
- account-scoped search

Use the KMP search/indexing engine. Do not create a second search database unless the existing architecture proves it necessary.

## Validation

Test large datasets, special characters, partial terms, empty queries, stale index, account switching, deleted messages, offline search, and performance.

**STOP AFTER PHASE 7.**

## 2026-10-18 — Current-design update

- Search: structured query all the way down (UI never builds SQL); on-device FTS index; every FTS term escaped (injection-proof by construction); debounce (~300ms); account-scoped; unified search joins enabled accounts only.
- Priority badges carry "why this priority?" explanations; classification carries "Why this category?" explanations.
- **No "Action Required" chip/filter/empty state in the Mail list** (removed on Android). Action Required remains available as a destination/section where defined, but not as a Mail-list chip. Search filter untouched.
