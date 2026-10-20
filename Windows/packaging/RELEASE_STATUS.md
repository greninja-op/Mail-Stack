# Mailstack Windows — RELEASE STATUS

**Decision: RELEASE BLOCKED**  
**Date:** 2026-10-20  
**Version under consideration:** `0.1.0` / MSIX `0.1.0.0` (never reuse a released version name)

## Why BLOCKED (observed)

| Blocker | Severity | Evidence on this PC |
|---------|----------|---------------------|
| MSVC linker (`link.exe`) missing | **Critical** | `cargo build` / `cargo test` fail: `linker 'link.exe' not found`. VS Build Tools / Community MSVC paths absent. |
| WinAppSDK / WinUI 3 packaging path incomplete | **Critical** | App is interim **egui/eframe** GUI-subsystem binary; Store-ready WinUI project not present. |
| Code-signing certificate absent | **Critical** | No Authenticode / MSIX signing cert observed; manifest Publisher is `CN=Mailstack-UNSIGNED-STUB`. |
| Windows SDK pack/sign tools missing | **Critical** | `MakeAppx` / `SignTool` **NOT FOUND** on PATH; Windows Kits bin tree missing. |
| Production icons / Store logos | **High** | `resources/icons/` placeholders only. |
| Live OAuth / Gmail smoke on release build | **High** | Not performed; Google app verification / CASA **not claimed**. |
| Clean release artifact inspection | **High** | No `.msix` produced. |
| Phase 28–29 live QA | **High** | Matrices document CODE-only; end-to-end **NOT RUN**. |

## Gate checklist (must all be PASS for READY)

- [ ] `cargo build -p mailstack-windows --release --target x86_64-pc-windows-msvc` succeeds
- [ ] `cargo test -p desktop-core --lib` and `cargo test -p mailstack-windows` pass
- [ ] MSIX staged, packed (`MakeAppx`), signed (`SignTool`) with production or trusted test cert
- [ ] Sideload install + launch smoke (light/dark, account connect or documented demo mode)
- [ ] No secrets in binary/logs; redaction verified
- [ ] Version bumped per `VERSIONING.md`; prior version not reused
- [ ] OAuth release client configured; verification status honestly documented
- [ ] Privacy disclosure reviewed; dependency review dated
- [ ] Human release owner explicitly flips this file to **RELEASE READY**

## What is ready in source (not a release)

- Shared `desktop-core` contracts through Phase 27 domains
- Windows interim shell + adapters (Credential Manager, browser, notifications stubs)
- MSIX **stubs** and packaging documentation
- QA matrices (Phases 28–29) with honest NOT RUN markers

## Rollback / update (when releases exist)

1. Never reuse `Identity@Version` in AppxManifest.
2. Document previous MSIX version in release notes; keep prior signed artifact for rollback sideload.
3. Local DB migrations are forward-only; test upgrade path before bumping migration version.
4. Credential Manager targets use prefix `Mailstack/` — account removal must delete those targets.

---

**Final line:** **RELEASE BLOCKED** — toolchain (MSVC + SDK), signing certificate, WinAppSDK packaging, and live smoke evidence are missing.
