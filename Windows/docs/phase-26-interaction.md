# Phase 26 — Desktop Interaction (Keyboard, Menus, Multi-Window)

**Status:** COMPLETE (interim egui) — 2026-10-20

## Delivered

`Windows/src/interaction.rs`:

- Shortcut map (refresh, search, settings, star, j/k, Ctrl+1–6 destinations, select-all, new window, Tab focus cycle).
- Menu bar (File/Edit/View/Account/Help) including Privacy Center + New window.
- Context menu helpers for mail rows.
- `SelectionState` — single + multi-select.
- `MultiWindowState` — deferred new-window request (host opens viewport).
- `FocusTarget` cycle — Sidebar → List → Conversation → Inspector → Overlay (no keyboard traps).
- Shell wires `handle_shortcuts_with` + interaction state.

## Tests

- selection clamp, focus cycle, multi-select toggle, new-window request.

## Blockers

- Native WinUI command bar / AppWindow multi-window lands with WinAppSDK packaging (Agent 5).
- Full egui launch needs MSVC toolchain on this host.
