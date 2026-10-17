# Mailstack — iOS (iPhone only)

Implementation root for the iPhone app. Sibling platform folders must not be created or modified from this workstream.

## Layout

```
iOS/
  CONTEXT.md          ← iOS-only continuity + skipped items (not root CONTEXT.md)
  project.yml         ← XcodeGen (generate .xcodeproj on Mac)
  Mailstack/          ← SwiftUI app sources
  MailstackTests/     ← Unit tests
  docs/               ← Phase notes
```

Shared KMP contracts (portable): `../shared/` (JVM target validated on Linux).

## Mac build

```bash
brew install xcodegen   # if needed
cd iOS && xcodegen generate
open Mailstack.xcodeproj
```

## Linux / Cloud Agent

Cannot run Simulator. Skips are recorded in `CONTEXT.md` in this folder.
