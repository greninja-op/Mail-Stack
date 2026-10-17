# Phase 0 — iOS Platform Architecture Audit & Boundary Foundation

**Status:** `[x]` complete and verified (documentation + boundary only)  
**Date:** 2026-10-18  
**Agent environment:** Linux Cloud Agent (no Xcode / no iPhone Simulator)

## Isolation confirmation

- Platform target: **iOS / iPhone only**
- Created: repository-root `iOS/` (+ root `editor-rules.md` isolation table)
- Did **not** create or modify `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, `Linux/`
- Did **not** redesign Android or implement Gmail/OAuth/mailbox features

---

## 1. Repository inventory

| Path | Role | Classification |
|------|------|----------------|
| `android/` | Full Mailstack Android app (Kotlin + Compose + Room) | EXISTING (Android-only) |
| `android/app/` | Single Gradle application module | EXISTING |
| `execution plan/iOS/` | iOS phase plan, requirements, design, editor rules | EXISTING (plan only) |
| `execution plan/android/` … other platforms | Other platform plans | EXISTING (untouched) |
| `CONTEXT.md` | Canonical continuity + standing rules R1–R64 | EXISTING |
| `iOS/` | iOS implementation + audit docs | **CREATED this phase** |
| Shared KMP module (`shared/`, `commonMain`, etc.) | Intended shared business core | **MISSING** |
| Apple Xcode project / `.xcodeproj` / `.xcworkspace` | iOS app shell | **MISSING** |
| Kotlin/Native iOS frameworks / XCFramework | SwiftUI bridge | **MISSING** |

### Android application (reference implementation)

- **Package:** `com.greninjaop.mailorganizer`
- **App name:** Mailstack
- **Version in tree:** 1.0.39 (versionCode 40)
- **Toolchain:** AGP 8.13.2, Kotlin 2.3.21, Compose BOM 2026.06.01, compileSdk/targetSdk 35, minSdk 26, Room 2.8.5
- **Kotlin sources (main):** 273 files — `core/` 60, `data/` 87, `domain/` 35, `ui/` 88, `di/` 1
- **Unit tests:** 121 test classes (suite last verified 1030 tests on this agent)
- **Layers present:** core (classify, company, search, priority, temporal, conversation, actions, rules, ai, privacy, cleanup, analytics, automation, email), domain use cases, data (Room, sync, auth, http, integrations, calendar, tasks), Compose UI

### Persistence / networking / auth (Android)

| Concern | Location | Notes |
|---------|----------|-------|
| Room DB | `data/local/` | Account-isolated schema; FTS5 search; migrations through v9+ |
| Sync | `data/sync/` | `SyncCoordinator`, cursors, workers, fixed `mailboxTotal` progress |
| OAuth | `data/auth/` | PKCE, EncryptedSharedPreferences, plain browser intent |
| Gmail HTTP | `data/http/`, sync/write clients | Direct HTTPS, not heavy Google client libs |
| Secure tokens | `SecureTokenStorage` | Android Keystore / EncryptedSharedPreferences |
| DI | `di/AppContainer.kt` | Manual container |

---

## 2. Current iOS readiness

| Capability | Status | Evidence |
|------------|--------|----------|
| iOS folder boundary | READY | `iOS/` created; root isolation rule written |
| Shared KMP module | **NOT READY** | No `kotlin("multiplatform")`, no `commonMain`, no iOS targets in Gradle |
| SwiftUI shell | **NOT READY** | No Xcode project |
| KMP → Swift bridge | **NOT READY** | No framework / XCFramework / SKIE / cinterop |
| Keychain / ASWebAuthenticationSession | **NOT READY** | No iOS code |
| Simulator build | **BLOCKED HERE** | This sandbox is Linux; no Xcode |
| Claim “iOS support” | **FORBIDDEN** | Plan: do not claim support merely because KMP *could* compile |

**Verdict:** iOS is at **audit / boundary foundation only**. Product iOS work starts at Phase 1 after a real shared module + Xcode shell exist (or an explicitly documented interim bridge — Phase 1 decides).

---

## 3. Critical finding — “KMP shared” does not exist yet

The execution plan assumes a Kotlin Multiplatform shared core consumed by Android + iOS.

**Actual repo state:** Mailstack Android is a **single Android application module**. Domain/core/data live inside `android/app` and depend on AndroidX / Android APIs in many places (especially `data/` — Room, WorkManager, EncryptedSharedPreferences, DataStore).

Rough Android API coupling (files importing `android.*` / `androidx.*`):

- `core/`: low (few files)
- `domain/`: low (few files)
- `data/`: high (~41 files)

**Implication for Phase 1+:**

1. Extract a true `shared` (or `mailstack-shared`) KMP module with `commonMain` (and later `iosArm64` / `iosSimulatorArm64`), **or**
2. Document an interim architecture (not preferred by the plan) — e.g. Swift reimplementation — which the plan forbids as a duplicate business engine.

Phase 0 does **not** perform that extraction. Phase 1 must establish the shell **and** the integration boundary; extraction may begin there only as far as the shell requires.

---

## 4. Shared API map (target for SwiftUI)

See [`kmp-integration-map.md`](kmp-integration-map.md) for the full list.

High-level consumers SwiftUI will need (once shared):

- Account / auth state (without tokens in UI)
- Mailbox observation (inbox destinations, threads, messages)
- Sync progress (fixed denominator, 99% cap while running)
- Classification / company / priority / search / rules / temporal / conversation / actions
- Integrations / privacy / analytics / AI (optional) abstractions
- Account isolation (`AccountId`) everywhere

Platform-owned on iOS (never push into shared as Android types):

- SwiftUI navigation & design tokens
- Keychain
- `ASWebAuthenticationSession` / URL scheme
- BGAppRefresh / notifications
- EventKit (Calendar) / Reminders adapters
- Info.plist entitlements & App Store privacy

---

## 5. iOS folder boundary (agreed layout)

```
iOS/
  README.md
  docs/                    # audits, status, iOS editor rules
  Mailstack/               # future Xcode app sources (Phase 1+)
  Mailstack.xcodeproj/     # future (Phase 1+) — not created in Phase 0
  Tests/                   # future
  design-reference/        # future animation/design studies if needed
```

Gradle/KMP shared module (when created) should live at repo root as e.g. `shared/` — **not** forked under `iOS/` — so Android and iOS both depend on it. Exact name/path is a Phase 1 decision; Phase 0 only requires that iOS-specific artifacts stay under `iOS/`.

---

## 6. Dependency / toolchain findings

| Item | Finding |
|------|---------|
| Android Kotlin | 2.3.21 — modern enough for KMP, but project is not multiplatform yet |
| Room / Compose / WorkManager | Android-specific — need expect/actual or iOS equivalents |
| HTTP | Custom `HttpTransport` — portable candidate for `commonMain` |
| Pure engines (`core/classify`, `core/search`, …) | Best first extraction candidates (minimize Android imports) |
| This agent host | Linux — cannot run `xcodebuild` or Simulator |
| Apple signing / App Store | Deferred to Phase 17–18 |

---

## 7. Security / privacy risks (carry into later phases)

- Tokens must use **Keychain**, never UserDefaults / ordinary SQLite columns / logs.
- OAuth: `ASWebAuthenticationSession` + redirect `com.greninjaop.mailorganizer:/oauth2redirect`; one sign-in grants Gmail+Calendar+Tasks (per Android R50).
- Email HTML is hostile — sanitize; no JS from mail; no body image fetches.
- Account isolation is mandatory (`AccountId`).
- No secrets in repo; no verbose token logging.
- AI remains optional, default off, never authorizes external actions.

---

## 8. Blockers

| Blocker | Severity | Owner |
|---------|----------|-------|
| No shared KMP module | **High** — blocks honest SwiftUI↔shared integration | Phase 1 |
| No Xcode project | **High** | Phase 1 |
| No macOS/Xcode on this Cloud Agent | **High for build/sim verification** | Need Mac CI or local Mac |
| Android `data` layer tightly coupled to AndroidX | **Medium** — slows extraction | Phase 1–2 |
| Live Google OAuth / device proof | **Known** — phone-only (CONTEXT R12) | Later phases |

---

## 9. Validation performed (honest)

| Check | Result |
|-------|--------|
| Read `CONTEXT.md`, iOS README/requirements/spec/design/editor-rules, Phase 0 | Done |
| Inspected repo roots and `android/` package tree | Done |
| Grepped for multiplatform / iOS Gradle targets | **None found** |
| Counted Kotlin sources / tests | 273 main / 121 test files |
| Sampled Android API usage in core/domain/data | Done |
| Created `iOS/` + root isolation rule | Done |
| Built Xcode / ran Simulator | **Not possible on this host** — not claimed |
| Implemented Gmail/OAuth/mailbox | **Not done** (out of Phase 0 scope) |

---

## 10. Files changed (this phase)

- `iOS/README.md` (new)
- `iOS/docs/editor-rules.md` (new)
- `iOS/docs/phase-00-architecture-audit.md` (this file)
- `iOS/docs/kmp-integration-map.md` (new)
- `iOS/docs/development-status.md` (new)
- `editor-rules.md` (new, root permanent isolation)
- `CONTEXT.md` (append Phase 0 completion — with remote merge)

---

## 11. Next phase recommendation

Proceed to **Phase 1 — iOS Application Shell & KMP Integration** when ready:

1. On a **macOS + Xcode** environment (or after provisioning one).
2. Establish shared KMP extraction plan (minimum `commonMain` surface for shell bootstrap).
3. Create Xcode/SwiftUI shell under `iOS/` with design tokens, navigation shell, loading/error/empty/offline primitives.
4. Wire a compile-time integration boundary (framework or equivalent) — stubs OK for mailbox/OAuth.
5. Validate Simulator launch there; do not claim phone verification until a real device run.

**STOP AFTER PHASE 0.**
