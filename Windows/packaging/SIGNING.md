# Code signing & MSIX signing

**Status:** Documentation only — **no signing performed on this PC** (no cert, no SignTool).

## Certificate types

| Use | Cert | Notes |
|-----|------|-------|
| Local sideload / CI smoke | Self-signed or org test CA | Install cert to Trusted People / root as required |
| Organization distribution | Org code-signing (EV preferred for SmartScreen) | Publisher CN must match AppxManifest `Identity Publisher` |
| Microsoft Store | Store association / Partner Center | Often re-signed by Store pipeline |

## Required tools

- Windows SDK: `MakeAppx.exe`, `SignTool.exe`
- Visual Studio Build Tools (MSVC) for the release binary

## Procedure (when tools + cert exist)

```powershell
# 1. Release binary
cargo build -p mailstack-windows --release --target x86_64-pc-windows-msvc

# 2. Stage package root (see scripts/stage-msix.ps1)
#    Fill AppxManifest.xml Publisher from the signing cert subject

# 3. Pack
MakeAppx pack /d .\packaging\msix\_stage /p .\packaging\msix\out\Mailstack.msix

# 4. Sign
SignTool sign /fd SHA256 /a /f <path-to-pfx> /p <pfx-password> .\packaging\msix\out\Mailstack.msix

# 5. Verify
SignTool verify /pa .\packaging\msix\out\Mailstack.msix
```

## Publisher identity

Stub today: `CN=Mailstack-UNSIGNED-STUB` in `AppxManifest.xml.stub`.  
Replace with the exact certificate subject (e.g. `CN=Your Org, O=..., C=US`) before packing.

## Security notes

- Never commit `.pfx` / private keys to the repo.
- Prefer CI secret stores / HSM for production signing.
- Do not distribute unsigned MSIX as a “release”.
