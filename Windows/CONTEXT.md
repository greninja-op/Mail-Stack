# Mailstack Windows — continuity (Windows boundary only)

Last updated: 2026-10-20

## Status
- **Phase 00:** COMPLETE (audit + gap list) — 2026-10-20
- **Phase 01+:** NOT STARTED (STOP after Phase 00 per plan)

## Standing (from execution plan/Windows)
- Rust-first shared desktop core (`desktop-core/`) reusable by macOS.
- Windows UI: **WinUI 3 / Windows App SDK + Fluent 2**, Rust-first (framework or safe bindings) — **not** a CLI product.
- Never modify `android/`, `iOS/`, `iPadOS/`, `AndroidTablet/`, `macOS/`, `Linux/` during Windows work.
- Follow `design.md` (2026-10-18) over older phase wording.

## Blockers for later phases (honest)
- MSVC / Visual Studio Build Tools (`link.exe`) required to compile native Windows targets on this PC.
- Live Google OAuth / Gmail verification needs client config + user account (not Phase 00).
