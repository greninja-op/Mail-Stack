# Mailstack iOS — Context (iOS only)

> **Scope:** Continuity log for the **iPhone / iOS** app under `iOS/`.  
> Do **not** merge into root `CONTEXT.md` (Android / product-wide).  
> Do **not** create or modify `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/`.

Last updated: 2026-10-17

---

## Standing rules (iOS)

1. All iOS source, assets, tests, docs, and this context live under `iOS/`.
2. Shared KMP (when present) lives in `shared/` — justified, portable only; never Android UI.
3. Follow `execution plan/iOS/` + `execution plan/iOS/design.md` + `iOS/docs/editor-rules.md`.
4. Never fake sync %; never auto-fetch mail-body images; tokens only in Keychain.
5. Demo accounts (`fixture-*`, `local-*`) never call Gmail sync.
6. Linux Cloud Agent: no Xcode / Simulator — document skips here honestly; do not claim device verification that did not run.
7. **Never edit root `CONTEXT.md` for iOS work** (user instruction 2026-10-17).

---

## Skipped / blocked (honest)

| Item | Phase | Why skipped / blocked | What exists instead |
|------|-------|------------------------|---------------------|
| Clean Xcode / Simulator build & launch | 1+ | Host is Linux; no Xcode / `xcodebuild` / Simulator | Swift sources + XcodeGen `project.yml` |
| Orientation / Dynamic Type / light-dark on device | 1+ | No Simulator | Tokens & layout written for HIG |
| XCFramework link of KMP → Swift | 1 | iOS KMP targets need macOS | `shared/` JVM commonMain; Swift ports mirror contracts |
| Real Google OAuth (ASWebAuthenticationSession) | 3 | No client secrets; no Simulator | AuthGate + Keychain + demo sign-in |
| Live Gmail API sync | 4+ | Needs OAuth + Mac | DemoSyncCoordinator; import UI with 99% cap |
| On-device SQLite / GRDB | 2 | Demo-first on Linux | `InMemoryStore` + PersistencePort swap point |
| Full company logo / favicon pipeline | 6 | No network asset pipeline wired | Letter avatars + domain field on rows |
| On-device FTS search index | 7 | Shared index not extracted yet | In-memory structured query filter |
| Full temporal / conversation intelligence UI | 8 | Engine not ported | RulesView seam |
| Live Calendar / Tasks APIs | 9 | Needs incremental OAuth scopes | IntegrationsView labels + ActionEngine demo |
| Real unified multi-account Gmail merge | 10 | No live accounts | Demo multi-account switcher |
| BGAppRefreshTask / notification delivery | 11 | No device | Info.plist fetch mode + OfflineSettingsView |
| App Store privacy nutrition form filing | 12 / 17 | No App Store Connect | Privacy settings + sanitize code |
| On-device AI model | 13 | Optional; off by default | StubAI |
| Advanced automation runner | 14 | Shared engine not on iOS yet | StubAutomation |
| VoiceOver / Reduce Motion on hardware | 15 | No Simulator | Labels, 44pt targets, CapabilityProfile |
| XCTest execution | 16 | Needs Xcode | `MailstackTests` sources authored |
| Code signing / provisioning / TestFlight | 17–18 | No Apple Developer credentials | Checklists in `docs/phase-02-through-18.md` |
| Root `CONTEXT.md` updates | — | Explicit user request | **This file only** |

---

## Decision log (iOS)

| Date | Decision |
|------|----------|
| 2026-10-17 | Phase 0 audit under `iOS/docs/`. Isolation: `iOS/` only. |
| 2026-10-17 | User: complete all iOS phases without waiting for approval; report when done. |
| 2026-10-17 | User: create **`iOS/CONTEXT.md`** for iOS skips; **do not** edit root `CONTEXT.md`. |
| 2026-10-17 | Scaffold Phases 1–18 in SwiftUI under `iOS/Mailstack/` with demo ports; `shared/` KMP JVM contracts. |

---

## Phase status

| Phase | Title | Status | Notes |
|-------|-------|--------|-------|
| 0 | Platform architecture audit | Done | `docs/phase-00-architecture-audit.md` |
| 1 | App shell & KMP integration | Source done | Simulator skipped |
| 2 | Local persistence | Source done | In-memory; SQLite skipped |
| 3 | OAuth / secure storage | Source done | Live OAuth skipped |
| 4 | Sync / import | Source done | Live Gmail skipped |
| 5 | Mailbox UI | Source done | Device UI skipped |
| 6 | Classification / companies | Source done | Logo pipeline partial |
| 7 | Search | Source done | In-memory |
| 8 | Rules / temporal | Source done | Seam |
| 9 | Action engine / Cal / Tasks | Source done | Live APIs skipped |
| 10 | Multi-account | Source done | Demo accounts |
| 11 | Offline / background | Source done | BG tasks skipped |
| 12 | Privacy | Source done | |
| 13 | AI | Source done | Stub off |
| 14 | Automation | Source done | Stub |
| 15 | A11y / adaptive | Source done | Hardware skipped |
| 16 | QA docs / tests | Source done | XCTest run skipped |
| 17 | Signing prep | Checklist only | Blocked |
| 18 | Release hardening | Checklist only | Blocked |

---

## Environment

- Agent host: Linux (Ubuntu), OpenJDK 17 — **not** macOS.
- Gradle: `shared/` JVM tests.
- Swift / Xcode: **not** available → Simulator validation **skipped**.

---

## How to use this file

1. After every iOS change: update **Phase status** and append **Skipped** rows if anything was deferred.
2. Never write iOS continuity into root `CONTEXT.md`.
3. When completed on a Mac, add a “Verified on Mac” subsection and clear the matching Skipped row.
