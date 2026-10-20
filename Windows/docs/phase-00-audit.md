# Phase 00 — Windows Architecture, Shared Rust Core & Desktop Audit

**Status:** COMPLETE (2026-10-20)  
**Scope:** Audit only. No Phase 01+ implementation in this deliverable.

## Required reading (done)

- `execution plan/Windows/README.md`
- `execution plan/Windows/requirements.md`
- `execution plan/Windows/spec.md`
- `execution plan/Windows/design.md` (2026-10-18 wins)
- `execution plan/Windows/editor-rules.md`
- Root `CONTEXT.md` standing rules (R1–R64) and root `editor-rules.md`

## Repository inventory (actual tree after scaffold wipe)

| Area | State |
|------|--------|
| `android/` | **EXISTING** — working Compose phone app (reference UI + product behaviour). Untouched this phase. |
| `desktop-core/` | **MISSING** — prior agent Rust core deleted by user-directed reset (2026-10-20). |
| `Windows/` | **NEW boundary** — this folder (audit docs + continuity only for Phase 00). |
| `shared/` (KMP) | **MISSING** — Android is a single-module app under `android/app`; no `commonMain` KMP library. |
| `iOS/`, `iPadOS/`, `AndroidTablet/`, `macOS/`, `Linux/` | **ABSENT** after reset (prior scaffolds removed). |
| Root `Cargo.toml` workspace | **ABSENT** after reset. |

### Android packages relevant to desktop reuse (reference, not copied)

Under `android/app/.../mailorganizer/`: `core/`, `data/`, `domain/`, `di/`, `ui/`.  
These hold sync, OAuth, classification, search, persistence, and UI. Desktop must **reuse semantics via a Rust `desktop-core`**, not paste Kotlin into Windows.

## Architecture decision (Phase 00)

1. **Create `desktop-core/` in Phase 01–02** as the shared Rust crate for platform-neutral domain/application/data (accounts, sync contracts, classification, search, privacy, pacing). Reusable by **macOS** and **Linux** per their plans.
2. **Windows UI path:** **WinUI 3 / Windows App SDK + Fluent 2**, with **Rust-first** presentation (Rust UI framework *or* safe bindings to WinUI). **CLI-only is not an acceptable product shape** under `design.md` / `editor-rules.md`.
3. **Do not** duplicate Android engines inside `Windows/` as a second business-logic tree.
4. **Do not** modify `android/` while executing the Windows plan (UI reference read-only).

## Design target (from design.md 2026-10-18 — must carry)

The Windows build must match current Android Mailstack behaviour/philosophy, including:

| Capability | Requirement |
|------------|-------------|
| Logo pipeline | bundled → app assets → favicon → letter; never fabricate |
| Sync progress | fixed denominator (`messagesTotal` once); Running caps 99%; Done = 100%; never backwards |
| Sync resume | page/cursor resume; do not advance past un-persisted pages |
| Throttle pacing | honour provider limits / Retry-After; do not silently skip throttled GETs (desktop requirement — Android phone today differs; do not claim phone parity) |
| Mail rows | flat Gmail-like rows; 40px avatar; unread bold; star independent of company pin |
| Navigation | desktop sidebar: All Inbox, Primary, Promotional, Social, Spam, Starred |
| Dark mode | charcoal `#131120`, never pure black |
| Privacy | no remote images in mail bodies; sanitize HTML; no JS from mail |

## Gap list (concrete)

| # | Gap | Needed for |
|---|-----|------------|
| G1 | No `desktop-core` Rust crate | Phases 01–02+ |
| G2 | No Windows project/toolchain (WinUI 3 + Rust bridge) | Phase 01 |
| G3 | No MSVC Build Tools (`link.exe`) observed on this PC | Local compile/launch |
| G4 | No OAuth/Credential Manager adapter | Phase 08–09 |
| G5 | No Gmail adapter / sync / persistence on desktop | Phases 07, 10–12 |
| G6 | No WinUI mailbox workspace UI | Phases 03–05, 13–14 |
| G7 | No packaging/signing pipeline | Phase 30 |
| G8 | No automated Windows UI tests yet | Later QA phases |

## Prior agent mistake (recorded)

A previous pass shipped a **CLI demo** under `Windows/` and claimed broad phase completion. That **violated** Windows `design.md` / `editor-rules.md`. Those scaffolds were **deleted** on 2026-10-20 by user direction. This Phase 00 restarts from the plan.

## Verification performed this phase

| Check | Result |
|-------|--------|
| Inspected repo after scaffold wipe | `android/` present; no `desktop-core` / no prior Windows sources |
| Confirmed `android/` file count still present | Yes (untouched) |
| Built/launched WinUI app | **Not applicable** — Phase 00 is audit only; no app yet |
| Live OAuth / Gmail | **Not claimed** |

## STOP

**STOP AFTER PHASE 00.** Do not start Phase 01 until the user directs the next Windows phase.
