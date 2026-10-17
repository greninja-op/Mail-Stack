# Phase 1 — Application Shell & KMP Integration

**Status:** Implemented in source (Simulator **skipped** on Linux — see `iOS/CONTEXT.md`).

## Delivered

- SwiftUI entry: `Mailstack/App/MailstackApp.swift`
- Navigation shell: drawer, tabs, 40pt profile, account switcher, settings panel (350ms), scroll-hide bars
- Design tokens: `Design/DesignTokens.swift` (brand violet, `#131120` dark bg, 28pt cards, motion)
- State primitives: loading / empty / error / offline
- DI boundary: `AppContainer` + ports (views do not talk to repositories)
- KMP shared contracts: repo `shared/` (JVM-validated commonMain)
- XcodeGen: `iOS/project.yml`

## Skipped here

- `xcodebuild` / Simulator install-launch (no macOS on this agent)
- Linking XCFramework into the Xcode target (needs Mac)
