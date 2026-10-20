# RELEASE STATUS

```
RELEASE BLOCKED
```

**Date:** 2026-10-20  
**Product:** Mailstack Windows (`Mailstack.exe`)

## Local build (observed 2026-10-20)

After installing Windows SDK **10.0.26100** (winget `Microsoft.WindowsSDK.10.0.26100`):

- `LIB` = Kits `Lib\10.0.26100.0\um\x64` + `ucrt\x64` + MSVC 2017 `lib\x64`
- `cargo build -p mailstack-windows` → **OK** (`Mailstack.exe`, GUI subsystem)
- `cargo test -p mailstack-windows` → **33 passed**
- `cargo test -p desktop-core --lib` → **95 passed**

## Blockers for distribution release

| Blocker | Detail |
|---------|--------|
| Code signing certificate | No production Authenticode / MSIX signing cert configured |
| Windows App SDK / WinUI 3 host | Not installed as packaging host; **egui is interim Rust-first shell only** |
| Store / enterprise identity | Package family name / publisher identity not finalized for signed MSIX |
| Live OAuth client | Production Google OAuth client + verification not wired (Phase 09+) |

## What is allowed now

- Local `cargo build` / `cargo test` of the interim egui GUI (with `LIB` set as above)
- Sideload **unsigned** MSIX stubs for layout experimentation only — **not** for distribution

## Exit criteria for UNBLOCKED

1. Valid signing cert + `signtool` / MSIX signing pipeline
2. WinAppSDK packaging path documented and reproducible on clean VS Build Tools
3. Smoke install on clean Windows 11 VM
4. Phase 28 matrix P/A for shipping scope; Phase 29 adversarial minimum set observed
