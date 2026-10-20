# MSIX packaging (Phase 30)

Mailstack ships as **MSIX** (sideload first; Store later). This folder holds stubs and mapping — **not** a signed package.

## Identity

| Field | Value |
|-------|-------|
| Display name | Mailstack |
| Package name | `Mailstack.Desktop` |
| Publisher (stub) | `CN=Mailstack-UNSIGNED-STUB` — replace before sign |
| Version scheme | `MAJOR.MINOR.PATCH.0` — see [`../VERSIONING.md`](../VERSIONING.md) |
| Architecture | `x64` (`x86_64-pc-windows-msvc`) |

## Layout

```
packaging/msix/
  AppxManifest.xml.stub
  mapping.txt.stub
  README.md
  _stage/                 # created by scripts/stage-msix.ps1 (gitignored preferred)
```

## Reproducible steps (when toolchain ready)

1. Open **x64 Native Tools** / VS Developer PowerShell so `link.exe` is on PATH.
2. `cargo build -p mailstack-windows --release --target x86_64-pc-windows-msvc`
3. `.\Windows\packaging\scripts\stage-msix.ps1 -ExePath .\target\x86_64-pc-windows-msvc\release\Mailstack.exe`
4. Copy final icons into `_stage\Assets\` (see `Windows/resources/icons/README.md`).
5. Edit staged `AppxManifest.xml` Publisher to match the signing certificate CN.
6. `MakeAppx pack /d .\Windows\packaging\msix\_stage /p .\Windows\packaging\msix\out\Mailstack.msix`
7. Sign per [`../SIGNING.md`](../SIGNING.md).

## Blockers on this machine (2026-10-20)

- `link.exe` / VS Build Tools missing
- `MakeAppx` / `SignTool` missing
- No signing certificate
- WinAppSDK project not yet the shipping UI (interim egui binary)

See [`../RELEASE_STATUS.md`](../RELEASE_STATUS.md): **RELEASE BLOCKED**.
