# Mailstack — Windows

Implementation root for the Windows desktop app (Rust + interim egui shell; WinUI 3 / WinAppSDK packaging path).

| Resource | Path |
|----------|------|
| Continuity / phase rollup | [`CONTEXT.md`](CONTEXT.md) |
| Execution plan | `execution plan/Windows/` |
| Shared core | `../desktop-core/` |
| QA matrices | [`docs/qa/`](docs/qa/) |
| Packaging / release gate | [`packaging/RELEASE_STATUS.md`](packaging/RELEASE_STATUS.md) |

Do not put Windows artifacts in sibling platform folders. Do not modify `android/` from this workstream.

## Prerequisites

1. **Rust stable** (see `rust-toolchain.toml`) with target `x86_64-pc-windows-msvc`
2. **Visual Studio Build Tools** (or VS) with **Desktop development with C++** so `link.exe` is available  
   - Open **x64 Native Tools Command Prompt** / Developer PowerShell before building
3. Optional for MSIX: Windows SDK (`MakeAppx`, `SignTool`) + signing certificate (see `packaging/SIGNING.md`)
4. Optional for live mail: set `MAILSTACK_OAUTH_CLIENT_ID` (and redirect if needed). **Never commit secrets.**

### This PC (2026-10-20)

MSVC / `link.exe` was **not** available — builds fail until Build Tools are installed.

## Build

From the **repo root** (Cargo workspace):

```powershell
# Ensure MSVC linker is on PATH first
cargo build -p mailstack-windows
cargo build -p mailstack-windows --release --target x86_64-pc-windows-msvc
```

Shared core only:

```powershell
cargo build -p desktop-core
cargo test -p desktop-core --lib
```

## Run

```powershell
cargo run -p mailstack-windows
```

The binary is a **GUI-subsystem** app (`Mailstack.exe`) — interim egui window with Fluent-inspired tokens. It is **not** a CLI product.

Without OAuth env + Gmail, UI paths use **demo / contract** data (categories, import-flow progress demo). That is expected until account connection is configured.

## Package (MSIX)

See [`packaging/msix/README.md`](packaging/msix/README.md). Staging helper:

```powershell
.\Windows\packaging\scripts\stage-msix.ps1 -ExePath .\target\x86_64-pc-windows-msvc\release\Mailstack.exe
```

**Current release decision: RELEASE BLOCKED** — do not distribute unsigned stubs as a release.

## Phase docs

- Audit: `docs/phase-00-audit.md`
- QA: `docs/phase-28.md`, `docs/phase-29.md`, `docs/phase-30.md`
