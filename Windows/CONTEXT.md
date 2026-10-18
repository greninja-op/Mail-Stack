# Mailstack Windows — Context (Windows only)

Last updated: 2026-10-18

## Verified this session
| Check | Result | Evidence |
|-------|--------|----------|
| `cargo test --manifest-path Windows/Cargo.toml` / `cargo test -p mailstack-windows` | **PASS** 3 tests | Session output |
| `cargo run --manifest-path Windows/Cargo.toml` | **PASS** prints `ok` | Session output |
| WinUI 3 / Fluent GUI shell | **BLOCKED** | **Biggest remaining item.** Next step: add a WinUI 3 (or egui/iced) crate under `Windows/` calling `desktop-core`. Needs Windows SDK / MSVC for native packaging. |
| MSI / Authenticode | **BLOCKED** | Windows SDK + cert |

## Discrepancy resolved
Earlier CONTEXT claimed `cargo test -p mailstack-windows` without a workspace — that form now works via root `Cargo.toml` workspace members.

## Phase status
0 done · 1–20 core wiring via CLI · 21–30 GUI + packaging blocked
