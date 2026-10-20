# Phase 03 — Application Shell, Lifecycle & State Restoration

**Status:** COMPLETE (interim egui shell) — 2026-10-20

## Architecture (honest)

| Layer | Choice |
|-------|--------|
| Interim GUI | `eframe` / `egui` multi-pane desktop window (`windows` subsystem) |
| Design tokens | Fluent-inspired; see `tokens.rs` + `design.md` |
| Packaging / native Fluent host | **WinUI 3 + Windows App SDK** when SDK installs — not claimed as shipped UI today |

This is **not** a CLI and **not** MessageBox-only.

## Delivered

- `shell.rs` — launch, chrome, lifecycle, load states
- `app_state.rs` — restorable selection + overlay state
- `workspace.rs` — column geometry + window size hydration each frame
- Launch / loading / ready / empty / offline / auth / error states

## Verification

- Unit tests: `shell_kind`, workspace column allocation
- `cargo build -p mailstack-windows` (see RELEASE_STATUS for SDK blockers)
