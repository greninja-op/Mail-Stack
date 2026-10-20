# Mailstack Windows — continuity (Windows boundary only)

Last updated: 2026-10-20 (UI shell + packaging/QA + phases 22–27)

## Status

| Phase | Status | Notes |
|-------|--------|-------|
| **00** | COMPLETE | Audit + gap list (`docs/phase-00-audit.md`) |
| **01** | COMPLETE | Toolchain, identity, workspace member, GUI subsystem |
| **02** | COMPLETE | `desktop-core` contracts + Windows adapters/bridge |
| **03–06** | COMPLETE | Shell, nav, tokens, bridge/logging |
| **07–21** | Other agents | Persistence → multi-account (see their docs) |
| **22** | COMPLETE | Background/offline/notifications — `docs/phase-22-background.md` |
| **23** | COMPLETE | Privacy Center + safe links + purge model — `docs/phase-23-privacy.md` |
| **24** | COMPLETE | AI router OFF by default — `docs/phase-24-ai-fallback.md` |
| **25** | COMPLETE | Finite automation + circuit breaker — `docs/phase-25-automation.md` |
| **26** | COMPLETE | Interaction (shortcuts/menus/focus/multi-window) — `docs/phase-26-interaction.md` |
| **27** | COMPLETE | Performance tiers + a11y hooks — `docs/phase-27-performance-a11y.md` |
| **14** | COMPLETE (demo UI) | Mail list/conversation/import chrome — `docs/phase-14-mail-workspace.md` |
| **28** | MATRIX DRAFTED | `docs/qa/phase-28-matrix.md` |
| **29** | CHECKLIST DRAFTED | `docs/qa/phase-29-adversarial.md` |
| **30** | **RELEASE BLOCKED** | `packaging/RELEASE_STATUS.md` — signing + WinAppSDK |

## Standing

- Rust-first shared desktop core (`desktop-core/`) reusable by macOS.
- **Interim UI:** egui/eframe multi-pane GUI with Fluent-inspired tokens from `design.md`.
- **Packaging path:** WinUI 3 / Windows App SDK + Fluent 2 (MSIX stubs under `packaging/msix/`).
- Never modify `android/`, `iOS/`, `iPadOS/`, `AndroidTablet/`, `macOS/`, `Linux/` during Windows work.
- `design.md` (2026-10-18) wins over older phase wording.
- Binary uses `#![windows_subsystem = "windows"]` — not a CLI product.

## Phases 22–27 (SUBAGENT 4)

### Shared (`desktop-core`)

- `background.rs` — explicit account on work; 15m clamp/staleness; `FailureClass` taxonomy; `NotificationDeduper`; `MemoryBackgroundScheduler`
- `privacy.rs` — `PrivacyCenterModel` tabs; `sanitize_log_line`; `launch_safe_link` (blocks javascript:/file:); `LocalPurgePlan` never touches Gmail
- `ai.rs` — `AiRouter` default OFF; `DataMinimizer`; injection shield; confidence cap 0.85; never downgrade SECURITY; `can_authorize_actions() == false`
- `automation.rs` — finite triggers/actions; account-scoped propose; circuit breaker; email cannot author rules
- `capability.rs` — integration capability registry + `PerformanceProfile` / `PerformanceTier` (no FPS-from-Hz)

### Windows

- `adapters/notifications.rs` — deduped show
- `adapters/browser.rs` — shared URL safety + `open_safe_link`
- `ui/privacy_center.rs` — tabbed Privacy Center
- `interaction.rs` — shortcuts, menus, context, selection, multi-window, focus cycle
- `capability_profile.rs` — wraps core performance profile + `AccessibilityHooks` (Narrator/HC/reduced-motion)
- `shell.rs` — wires interaction + a11y re-evaluation

## Verified (2026-10-20, SUBAGENT 4)

With ScopeCppSDK `vc15` `link.exe` + `LIB` on PATH:

- `cargo test -p desktop-core --lib` filters: **background 7**, **privacy 5**, **ai 6**, **automation 4**, **capability 4** — all ok
- `cargo test -p mailstack-windows --bins --tests` — **33 passed**, 0 failed

## Honest blockers

1. **Code signing + WinAppSDK packaging host** → **RELEASE BLOCKED** (`packaging/RELEASE_STATUS.md`).
2. **WinRT toast / native multi-window** deferred to WinAppSDK packaging.
3. **Live OAuth/Gmail** and persistence-backed purge still need client config + Phase 07 maturity.
4. Local link: Windows SDK **10.0.26100** `Lib\…\um\x64` + `ucrt\x64` + MSVC `lib\x64` on `LIB` (installed this session); keep that for reproducible builds.

## Key modules (22–27)

- `desktop-core/src/{background,privacy,ai,automation,capability}.rs`
- `Windows/src/{interaction,capability_profile}.rs`
- `Windows/src/adapters/{notifications,browser}.rs`
- `Windows/src/ui/privacy_center.rs`
- `Windows/docs/phase-22-*.md` … `phase-27-*.md`
