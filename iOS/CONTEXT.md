# Mailstack iOS — Context (iOS only)

> Continuity for **iPhone / iOS** under `iOS/`. Do **not** merge into root `CONTEXT.md`.

Last updated: 2026-10-18

## Standing rules
1. All iOS artifacts under `iOS/`. Shared KMP in `shared/` only when portable.
2. Demo accounts (`local-*`, `fixture-*`) never sync. Progress caps at 99% while Running; Done = 100%.
3. Never fetch mail-body images. Tokens only in Keychain.
4. Import travelling-pulse animation: **HELD — do not build**.
5. Sync pacing (token bucket ~10/sec) is a **new-platform requirement**, not Android phone behaviour.

## Verified this session (2026-10-18)

| Check | Result | Evidence |
|-------|--------|----------|
| `shared/` `./gradlew jvmTest` (clean + rerun) | **PASS** 7 tests (3 AccountId + 4 SyncProgress) | XML under `shared/build/test-results/jvmTest/` |
| iOS XCTest (4 + new) | **BLOCKED** | No Mac/Xcode on this Linux agent. Unblock: macOS runner / local Mac `xcodegen generate && xcodebuild test` |
| Simulator UI | **BLOCKED** | Same |

## Skipped / blocked (exact unblockers)

| Item | Unblocker |
|------|-----------|
| XCTest / Simulator / XcodeGen generate+build | Mac + Xcode |
| Live Google OAuth / Gmail / Calendar / Tasks | OAuth client secrets + test user + Mac |
| SQLite replacing InMemoryStore | Implement GRDB/SQLite under `iOS/` then re-run persistence tests |
| Company logo pipeline / FTS | Wire asset+favicon pipeline + FTS index on Mac |
| BGAppRefreshTask on device | Physical device + push entitlement |
| App Store / TestFlight / signing | Apple Developer account |
| XCFramework link of KMP iOS targets | Enable `iosArm64`/`iosSimulatorArm64` on Mac; produce XCFramework |
| Import animation | User go-ahead after Gemini prompt (HELD) |

## Phase status
| Phase | Status |
|-------|--------|
| 0–16 | Source scaffold + unit tests authored |
| 17–18 | Checklist only |

## Decisions
| Date | Decision |
|------|----------|
| 2026-10-17 | Platform-local CONTEXT only; root CONTEXT untouched. |
| 2026-10-18 | Re-verified shared jvmTest from clean rebuild; stale PASS claims for XCTest removed. |

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
