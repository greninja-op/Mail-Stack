# Phases 2–18 — Implementation notes (iOS)

All implementation lives under `iOS/`. Honest skips are listed in `iOS/CONTEXT.md` only (root `CONTEXT.md` is not updated for iOS).

| Phase | Delivered under `iOS/` | Primary skip |
|------:|------------------------|--------------|
| 2 | `Data/Local/*` InMemoryStore + PersistencePort | On-device SQLite / GRDB not wired (demo memory store) |
| 3 | `Data/Auth/KeychainTokenStore` + DemoAuth + AuthGate | Live Google OAuth round-trip |
| 4 | `DemoSyncCoordinator` + ImportFlowView (99% cap) | Live Gmail API sync |
| 5 | MailListView, MessageDetailView, MailRowView, drawer destinations | Simulator UI verification |
| 6 | DemoClassification + CategoriesView + CategoryGroupCard | Full company logo pipeline / favicon fetch |
| 7 | SearchView structured query + 300ms debounce | On-device FTS index (in-memory filter) |
| 8 | RulesView seam | Full temporal / conversation engine UI |
| 9 | ActionsView + IntegrationsView (Calendar/Tasks labels) | Live Calendar/Tasks API |
| 10 | AccountSwitcherSheet + multi demo accounts | Unified inbox merge across real accounts |
| 11 | OfflineSettingsView + Info.plist background fetch | BGAppRefreshTask scheduling on device |
| 12 | DefaultPrivacy sanitize / no remote images; Keychain; wipe | Penetration test / App Store privacy nutrition labels filing |
| 13 | StubAI off-by-default + AISettingsView | On-device model |
| 14 | StubAutomation + AutomationView | Advanced rule runner |
| 15 | CapabilityProfile + a11y labels / 44pt targets | VoiceOver / Reduce Motion on hardware |
| 16 | `MailstackTests` + this matrix | Full adversarial QA on Simulator |
| 17 | Docs checklist below | Signing, provisioning, App Store Connect |
| 18 | Release checklist below | TestFlight / production cut |

## Phase 17 — Signing checklist (docs only)

- [ ] Apple Developer team selected in Xcode
- [ ] Bundle ID `com.greninjaop.mailorganizer` registered
- [ ] URL scheme `com.greninjaop.mailorganizer` for OAuth redirect
- [ ] Google Cloud OAuth iOS client + reversed client ID
- [ ] App Store privacy nutrition labels drafted
- [ ] Push / background modes justified in review notes

## Phase 18 — Release hardening checklist (docs only)

- [ ] No tokens in logs (SafeDiagnostics)
- [ ] Demo accounts cannot sync
- [ ] ATS denies arbitrary loads
- [ ] Release scheme archive on Mac
- [ ] Smoke: cold launch → demo inbox → search → settings wipe
