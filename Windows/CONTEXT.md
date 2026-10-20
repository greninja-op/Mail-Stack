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

## Execution-plan fill (2026-10-20)

Went through `execution plan/<platform>/` and implemented everything feasible **without** live Google OAuth verification, Mac Simulator, or store signing.

**Added this session (high level):**
- `desktop-core`: persistence, oauth contracts, classification, multi-account (+ tests) — **23 cargo tests PASS**
- Windows/Linux: `workspace` façade over core (inbox/search/classify/oauth/privacy contracts)
- iOS: SQLite/file persistence seam, PKCE/OAuth mocks, token-bucket pacer, FTS builder, log redactor, AI minimizer, automation store, QA/signing checklists, extra XCTests (Mac-blocked to run)
- shared KMP: OAuthContracts + TokenBucket (+ 2 JVM tests) — **9 jvmTest PASS** this session
- AndroidTablet: pacer, HtmlSanitizer, StructuredSearch + unit tests (SDK missing in this VM — run when SDK present)
- iPadOS/macOS: OAuth+pacer seams
- Every platform: `docs/phase-status.md` for all phases

**Still BLOCKED (by design / environment):**
- Live OAuth round-trip / Gmail / Calendar / Tasks
- XCTest / Simulator / XcodeGen builds (no Mac)
- WinUI / GTK native shells
- App Store / Play / notarization / MSI / Flatpak
- AndroidTablet Gradle in this VM if Android SDK absent
