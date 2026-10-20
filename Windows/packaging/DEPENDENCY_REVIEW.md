# Dependency review (Phase 30)

**Date:** 2026-10-20  
**Scope:** Workspace crates `desktop-core`, `mailstack-windows` (Cargo.toml)

## Notable dependencies

| Crate | Role | Notes |
|-------|------|-------|
| `serde` / `thiserror` | Contracts | No network |
| `sha2` / `base64` / `url` | OAuth PKCE / URL | Local crypto helpers |
| `regex` | Log/HTML helpers | Keep patterns reviewed |
| `uuid` | IDs | |
| `eframe` / `egui` | **Interim** GUI | Replace/bridge toward WinUI for Store path |
| `windows` (Win32) | CredMan, ShellExecute | Feature-gated; review on upgrade |
| `log` | Logging | Use redacting logger only |

## Findings

1. No telemetry SDKs observed in Windows/desktop-core Cargo manifests.
2. No hardcoded API keys in Cargo features.
3. `eframe` pulls a large transitive GUI stack — acceptable for interim; revisit license/size before Store.
4. Full `cargo deny` / advisory audit **NOT RUN** (build blocked without MSVC).

## Before RELEASE READY

- [ ] `cargo deny check` or `cargo audit` on release lockfile
- [ ] Confirm no debug-only bypass features in `--release`
- [ ] Re-review `windows` crate feature set (least privilege)
