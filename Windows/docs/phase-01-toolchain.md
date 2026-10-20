# Phase 01 — Windows Target & Toolchain Foundation

**Status:** COMPLETE (2026-10-20)  
**Boundary:** `Windows/` only. Does not modify `android/` or sibling platforms.

## Delivered

| Item | Location / notes |
|------|------------------|
| Workspace member | Root `Cargo.toml` → `members = ["desktop-core", "Windows"]` |
| Package | `Windows/Cargo.toml` — bin `Mailstack` |
| Toolchain pin | `Windows/rust-toolchain.toml` — stable + `x86_64-pc-windows-msvc` |
| Identity | `Windows/src/identity.rs` — display name **Mailstack** |
| GUI subsystem | `#![windows_subsystem = "windows"]` in `src/main.rs` |
| Resources / MSIX stubs | `Windows/resources/`, `Windows/packaging/msix/` |
| Build profiles | Workspace `dev` / `release` in root `Cargo.toml` |
| Secrets | None in source |

## UI path (honest)

- **Interim shell:** egui / eframe (Rust-first multi-pane GUI) — Phase 03+.
- **Packaging / design target:** WinUI 3 / Windows App SDK + Fluent 2 when the SDK is available.
- Product shape is a **GUI window**, never a console CLI.

## MSVC / LIB note (this machine)

`link.exe` may exist while Windows Kits `Lib` (and `kernel32.lib`) is incomplete. Before `cargo build`:

1. Put MSVC `Hostx64\x64` on `PATH` (so `link.exe` resolves).
2. Set `LIB` to MSVC `lib\x64` plus a folder that contains `kernel32.lib` (full Windows 10/11 SDK `um\x64` + `ucrt\x64`, or — on this PC — VS 2022 ScopeCppSDK `SDK\lib` as a temporary workaround).

## Verification

- `cargo check -p mailstack-windows` / `cargo test -p mailstack-windows` after PATH + LIB are set.
