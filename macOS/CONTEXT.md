# Mailstack macOS — Context (macOS only)

Last updated: 2026-10-18

## Verified this session
| Check | Result | Evidence |
|-------|--------|----------|
| `cargo test -p mailstack-desktop-core` | **PASS** 18 tests | Session cargo output |
| Xcode / Swift compile / MacStoreTests | **BLOCKED** | No Mac. Unblock: Mac + XcodeGen + Xcode |
| UniFFI bridge to desktop-core | **BLOCKED** | Biggest remaining item — next step: generate UniFFI bindings on Mac and replace `MacStore` demo mirror |
| Live OAuth / notarization | **BLOCKED** | Secrets + Apple Developer ID |

## Phase status
0 done · 1–15 SwiftUI scaffold · 16–30 checklists · UniFFI pending

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
