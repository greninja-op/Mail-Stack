# Phase 30 — Production Signing, Installer & Final Release Hardening

**Status:** COMPLETE (packaging docs + explicit release gate) — 2026-10-20  
**Release decision:** **RELEASE BLOCKED** — see [`../packaging/RELEASE_STATUS.md`](../packaging/RELEASE_STATUS.md)

## Deliverables

| Artifact | Path |
|----------|------|
| Release gate | `Windows/packaging/RELEASE_STATUS.md` |
| Version scheme | `Windows/packaging/VERSIONING.md` |
| Signing runbook | `Windows/packaging/SIGNING.md` |
| MSIX stubs + README | `Windows/packaging/msix/` |
| Stage helper (doc script) | `Windows/packaging/scripts/stage-msix.ps1` |
| Privacy / data disclosure stub | `Windows/packaging/PRIVACY_DISCLOSURE.md` |
| Dependency review notes | `Windows/packaging/DEPENDENCY_REVIEW.md` |

## Observed blockers (this PC)

1. **MSVC / `link.exe`** — required for `x86_64-pc-windows-msvc` release builds; not installed.
2. **WinAppSDK / WinUI 3** — product packaging path not wired; current binary is interim egui.
3. **Code-signing certificate** — none present; Publisher remains stub `CN=Mailstack-UNSIGNED-STUB`.
4. **MakeAppx / SignTool** — Windows SDK packaging tools not on PATH.
5. **Production icons / Store assets** — placeholders only under `resources/icons/`.
6. **Google OAuth verification / CASA** — not started; restricted Gmail scopes need assessment before public verification (do not claim verified).
7. **Clean release build + MSIX smoke install** — not performed.

## Explicit decision

**RELEASE BLOCKED.** Do not publish, sideload as production, or submit to Microsoft Store until the gate checklist in `RELEASE_STATUS.md` is green and a human signs off.
