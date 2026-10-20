# Phase 28 — Functional & Integration QA Matrix

**Date:** 2026-10-20  
**Machine:** Athira Aswin Windows 10/11 PC (this workspace)  
**Rule:** Only claims with observed evidence. Unrun items are marked **NOT RUN**.

## Environment observed (this PC)

| Check | Result (observed) |
|-------|-------------------|
| `cargo --version` | `cargo 1.98.0` present |
| `rustc --version` | `rustc 1.98.0` present |
| `link.exe` (MSVC) | **NOT FOUND** on PATH; VS Build Tools / Community MSVC trees **MISSING** |
| Windows SDK `MakeAppx` / `SignTool` | **NOT FOUND** |
| WinAppSDK / WinUI 3 project | **Not present** — interim **egui/eframe** shell |
| Live Google OAuth / Gmail | **NOT PERFORMED** |
| MSIX install / sideload | **NOT PERFORMED** |
| `cargo test` / `cargo build` | **FAILED** — `error: linker 'link.exe' not found` (observed 2026-10-20) |

**Verdict for Phase 28 live QA:** blocked on MSVC linker. Matrix below records **code-present vs executed**.

Legend: **PASS** = executed and observed OK · **FAIL** = executed and failed · **CODE** = implemented in tree, not executed · **GAP** = missing / stub / demo-only · **N/A** = not applicable yet

---

## A. Install / upgrade / restart

| # | Case | Status | Evidence |
|---|------|--------|----------|
| A1 | Clean install (MSIX) | **NOT RUN** / **GAP** | Packaging stubs only; no signed MSIX |
| A2 | Upgrade preserves local DB + credentials | **NOT RUN** | No install artifact |
| A3 | App restart restores window layout | **CODE** | `AppState` / shell layout persistence present; GUI not launched |
| A4 | Sleep/wake resume | **NOT RUN** | No live session |

## B. OAuth & account connection

| # | Case | Status | Evidence |
|---|------|--------|----------|
| B1 | PKCE S256 auth URL shape | **CODE** | `desktop-core/src/oauth.rs` unit tests present; **not executed** (link.exe) |
| B2 | Live Google sign-in | **NOT RUN** | No `MAILSTACK_OAUTH_CLIENT_ID` exercised; no browser OAuth observed |
| B3 | Token storage in Windows Credential Manager | **CODE** | `Windows/src/adapters/credential_windows.rs`; WinCred path untested live |
| B4 | Auth failure / recovery UX | **NOT RUN** | |
| B5 | Permission denial | **NOT RUN** | |

## C. Sync / pacing / import flow

| # | Case | Status | Evidence |
|---|------|--------|----------|
| C1 | Fixed denominator; running ≤99%; Done=100%; never backwards | **CODE** | `sync.rs` + bridge demo snapshot tests in source |
| C2 | Page/cursor save-after-persist | **CODE** | `persistence.rs` tests in source |
| C3 | Throttle / Retry-After honour | **CODE** | `pacing.rs` / `gmail.rs` contracts |
| C4 | Live initial Gmail sync | **NOT RUN** | |
| C5 | Incremental / historyId recovery | **CODE** | `sync_engine.rs` |
| C6 | Import-flow UI progress chrome | **CODE** | `ui/import_flow.rs` uses **demo** snapshot — not live sync |

## D. Mail parsing / HTML / logos

| # | Case | Status | Evidence |
|---|------|--------|----------|
| D1 | HTML sanitize (scripts, remote images) | **CODE** | `html.rs` tests; see also Phase 29 |
| D2 | Logo pipeline (bundled→assets→favicon→letter) | **CODE** | `logo.rs` |
| D3 | Malformed MIME / HTML cases | **PARTIAL CODE** | Coarse sanitizer only; no full MIME parser live-tested |

## E. Categories / companies / search / priority / rules

| # | Case | Status | Evidence |
|---|------|--------|----------|
| E1 | Category groups + UI cavity | **CODE** | `category.rs`, `ui/categories.rs` (demo groups) |
| E2 | Company intelligence / pin vs star | **CODE** | `company.rs` |
| E3 | Local search index | **CODE** | `search.rs` |
| E4 | Priority / Action Required explainability | **CODE** | `priority.rs`, `actions.rs` |
| E5 | Rules + user corrections | **CODE** | `rules.rs` |
| E6 | Live UI verification light/dark + scaling | **NOT RUN** | Shell not launched |

## F. Temporal / conversation / Calendar / Tasks

| # | Case | Status | Evidence |
|---|------|--------|----------|
| F1 | Temporal extraction | **CODE** | `temporal.rs` |
| F2 | Conversation intelligence | **CODE** | `conversation.rs` |
| F3 | Action cards → Calendar/Tasks proposals | **CODE** | `actions.rs` + capability scopes |
| F4 | Live Calendar/Tasks write | **NOT RUN** | Scopes contracted; no live provider call |

## G. Multi-account / offline / notifications / automation / AI

| # | Case | Status | Evidence |
|---|------|--------|----------|
| G1 | Unified inbox receiving-account indicator | **CODE** | `multi_account.rs` |
| G2 | Demo accounts never sync | **CODE** | `assert_may_sync` |
| G3 | Offline / background work | **CODE** | `background.rs` |
| G4 | Notifications | **CODE** | adapters + contracts; toast delivery **NOT RUN** |
| G5 | Automation confirmation + circuit breaker | **CODE** | `automation.rs` |
| G6 | Optional AI disabled by default | **CODE** | `ai.rs` |
| G7 | Account-isolation attack (live) | **NOT RUN** | Unit coverage in core; no multi-account live UI |

## H. Interaction / a11y / performance (Phase 26–27)

| # | Case | Status | Evidence |
|---|------|--------|----------|
| H1 | Keyboard shortcuts / menus | **CODE** | `interaction.rs` |
| H2 | Reduced motion / capability profile | **CODE** | `capability_profile.rs` |
| H3 | Accessibility / screen reader | **NOT RUN** | egui interim; WinUI Narrator path not wired |
| H4 | Frame-time / jank inspection | **NOT RUN** | |
| H5 | Light/dark charcoal theme tokens | **CODE** | `tokens.rs` |

## I. Screenshot / regression review

| # | Case | Status | Evidence |
|---|------|--------|----------|
| I1 | Screenshot review major UI | **NOT RUN** | No screenshots captured this phase |
| I2 | Regression of existing screens | **NOT RUN** | |

---

## Summary counts (this PC, 2026-10-20)

| Bucket | Count (approx.) |
|--------|-----------------|
| Executed PASS | **0** end-to-end UI / OAuth / sync |
| Executed FAIL (toolchain) | **`cargo test`/`build` — link.exe missing** |
| CODE present, not executed | Majority of phases 01–27 contracts + egui shell |
| GAP / stub | WinUI3 packaging path, production icons, signed MSIX, live OAuth |

**Phase 28 outcome:** QA matrix **documented**. Full functional/integration sign-off **BLOCKED** until MSVC + live OAuth environment available.
