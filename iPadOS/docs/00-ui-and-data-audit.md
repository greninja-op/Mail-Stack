# iPadOS UI and data audit

Evidence from the repo on 2026-10-21. This machine is Windows. Xcode is not installed here, so this audit does not claim a simulator build, an install, or a launch.

`design.md` (2026-10-18) wins over older phase files where they disagree.

## Data boundary

Phone business data already lives in the Android Room/domain code.

- Persistence: `android/app/src/main/java/com/greninjaop/mailorganizer/data/local/AppDatabase.kt` is a Room database (`androidx.room.RoomDatabase`), schema version 10. Entities cover accounts, threads, messages, senders, companies, classifications, priorities, action items, sync state, user rules and corrections, extracted items, search-index metadata, integration state, and automation. DAOs sit beside it under `data/local`.
- Domain: use cases live under `android/app/src/main/java/com/greninjaop/mailorganizer/domain/` (classify, search, rules, actions, sync-facing mail fetch, calendar, tasks, and the rest). They are Kotlin/JVM code inside the Android `:app` module.
- Gradle: `android/settings.gradle.kts` includes only `:app`. `android/app/build.gradle.kts` applies the Android application, Kotlin Android, Compose, and KSP plugins. There is no `kotlin-multiplatform` plugin and no shared module.

There is no KMP framework in this repo yet. A future shared Kotlin module would be the backend boundary, and it is not wired. This audit does not add a Gradle KMP module, a second database, or a sync engine.

The iPad UI uses a local demo mailbox. The phone SwiftUI target already does that in memory (`iOS/Mailstack/Model/DemoMailbox.swift`: seeded rows, not Gmail and not a database). The iPad target does not open Room and does not copy those rows into a store. `MailstackPadApp` shows `Text("Mailstack")` until the shell exists. `PadShell` is agent 2. This target does not stub it.

## Phone shell (iOS)

Source: `iOS/Mailstack/Navigation/RootShell.swift`, `ChromeLayout.swift`, `MoBars.swift`, and `iOS/docs/safe-area-audit.md`.

The phone is a single column. `MoNavBar` is a bottom tab bar. Its content height is 80pt (`MailstackColors.barContentHeight`). `RootShell` puts the live window bottom inset **outside** that 80pt (`.padding(.bottom, bottomInset)`). `ChromeLayout.contentBottomPadding` is `max(0, systemBottomInset) + 80pt * barsAmount`. When the bar hides on scroll, only the 80pt collapses. The system inset stays, so the last row cannot slide under the home indicator.

`ChromeLayout.currentWindowInsets()` reads `UIWindow.safeAreaInsets`. An inset of 0 (no home indicator, or the indicator hidden) means no leftover gap. Nothing adds a constant 34pt or 48pt on top of the measured inset. The top bar is 64pt of content plus the status-bar inset, and that whole top padding collapses with the bars.

Labels and folders also open from a hamburger drawer (`MailDrawer`). The bottom bar is the phone's primary section switch (Home, Mail, Categories, Companies, Actions). That bottom bar is an iPhone layout. It is not the iPad navigation.

## Gmail on iPad (familiarity, not a clone)

Gmail on iPad is the familiarity reference in `execution plan/iPadOS/design.md`. In landscape the message list sits beside the open message. Portrait, and a narrow Split View or Slide Over, drop to one column. Labels live in a left menu, not a bottom tab bar. Mailstack keeps its own identity: violet `#5238E8` / `#B9A8FF`, background `#F6F3FF` / `#131120`, flat mail rows, 28pt cards. No Liquid Glass redesign. `PadColors` maps those tokens.

## iPad plan

Source: `execution plan/iPadOS/design.md`. Width decides the layout. Device-name checks are not the rule.

| Width | Columns | Navigation |
| --- | --- | --- |
| Wide (landscape, full screen, medium/wide split) | sidebar \| list \| detail | Sidebar stays on screen. Destinations: All Inbox, Primary, Promotional, Social, Spam, Starred. |
| Narrow (portrait, narrow Split View, Slide Over) | one column | Left menu becomes a drawer. One column at a time, same rows and groups as the phone contract. |

`design.md` also allows an optional inspector beside the detail at wide widths (action cards, "why this category/priority"). It is contextual and disappears when the width is narrow. The baseline workspace is still sidebar \| list \| detail.

Selection stays put when the window resizes. Bars, when a screen has them, overlay and translate on scroll (250ms). They are not a second padded container.

### Same live safe-area rule

The iPad shell uses the same inset math as `ChromeLayout`, even though labels are not a bottom tab bar:

- Read the live window safe-area insets. Do not hardcode 34pt, 48pt, or an 80pt gap.
- Any chrome that sits on the screen edge keeps the measured inset **outside** its content height.
- Inset 0 means no leftover gap. Fullscreen and windows with no home-indicator inset sit on the edge.
- When scroll hides a bar, only the bar's content height collapses. The system inset stays reserved.

## What this scaffold is

| Path | Role |
| --- | --- |
| `iPadOS/MailstackPad.xcodeproj/project.pbxproj` | Xcode 16 project (`objectVersion` 77, `compatibilityVersion` Xcode 16.0). `PBXFileSystemSynchronizedRootGroup` path `MailstackPad` (folder `iPadOS/MailstackPad`). Deployment target iOS 17. `TARGETED_DEVICE_FAMILY = 2`. Bundle id `com.greninjaop.mailstack.ipad`. Portrait and landscape are allowed so Split View and landscape can show the columns above. |
| `iPadOS/MailstackPad/MailstackPadApp.swift` | `@main` app. Temporary root is `Text("Mailstack")`. |
| `iPadOS/MailstackPad/Theme/PadColors.swift` | Brand, background, surfaces, category accents, 28pt card radius. |
| `iPadOS/docs/00-ui-and-data-audit.md` | This audit. Not part of the app target. |

Not built here: no iPad simulator run, no device run, no Gradle change, no `PadShell`.
