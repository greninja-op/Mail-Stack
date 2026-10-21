# Phase 00 — Android tablet UI and data audit

Audit date: 2026-10-21. Source of truth for the current phone UI is `android/app/src/main/java/com/greninjaop/mailorganizer/ui`. Visual contract: `execution plan/AndroidTablet/design.md` (2026-10-18). Where that contract still describes an older destination list or turns dynamic color on, the phone code and this note win for the tablet shell.

No emulator was launched for this audit. This machine may not have launched an emulator. No device install is claimed.

## Data decision

The phone app already has Room and domain code under `android/`. `AppDatabase` (`android/app/src/main/java/com/greninjaop/mailorganizer/data/local/AppDatabase.kt`) is schema version 10. Accounts, mail, classification, search, rules, and sync state live there. OAuth and Gmail sync also live in that phone module.

The tablet presentation does not build a second database, a second OAuth client, or a second Gmail sync. `AndroidTablet/` is a Compose UI module only (`applicationId` `com.greninjaop.mailstack.tablet`). It reads a local demo mailbox until a shared module is intentionally wired. Phone `SampleMailboxSeeder` stays in the phone Room database and is not copied here.

## Phone bottom bar destinations

`PrimaryDestination` in `ui/navigation/MoNavBar.kt`, left to right:

1. Home (`Icons.Filled.Home`)
2. Mail (`Icons.Filled.Email`)
3. Categories (`Icons.Filled.List`)
4. Companies (`Icons.Filled.AccountBox` — core icons have no Business glyph; the label carries the meaning)
5. Actions (`Icons.Filled.CheckCircle`)

`design.md` still lists All Inbox, Primary, Promotional, Social, Spam, Starred. The shipped phone bar does not. Mailstack tablet uses these five destinations. It does not use Gmail’s Chat or Meet.

The bar is one hoisted instance above the NavHost (`AppNavGraph.kt`) so the jelly pill slides instead of snapping on tab change. Scroll down hides it; scroll up shows it. Entering a tab always shows the bars.

## Bar geometry and list padding

`MoNavBar` content height is **80dp** (`BarHeight`). `navigationBarsPadding()` is applied **outside** that 80dp:

```text
Modifier.fillMaxWidth().navigationBarsPadding().height(80.dp)
```

Total bar height is 80dp plus the live system navigation inset. The Scaffold adds no extra window insets.

Primary list padding is `PrimaryContent` in `AppNavGraph.kt`:

```text
bottom = navBar + 80.dp * barsOffset
```

`navBar` is the live `WindowInsets.navigationBars` bottom inset. Only the 80dp app bar is multiplied by `barsOffset` (0 hidden, 1 visible). The system inset is never multiplied by `barsOffset`, so the last row stays above the system navigation buttons when the app bar hides. Top padding is `(statusBar + 64.dp) * barsOffset` for the hoisted top bar.

## Violet tokens

Copied from `ui/theme/Color.kt` into `TabletTheme.kt`:

| Role | Light | Dark |
| --- | --- | --- |
| Brand primary | `#5238E8` | `#B9A8FF` |
| Background | `#F6F3FF` | `#131120` (violet-tinted charcoal, not pure black) |
| On-surface | `#1C1A26` | `#F0EDFB` |

`Color.kt` documents dynamic color as off so every device stays violet. `Theme.kt` (`MailOrganizerTheme`) later turns it **on** for API 31+ (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) and uses the violet schemes only below Android 12. `design.md` also says dynamic color is on.

Tablet theme follows `Color.kt` and this phase: **dynamic color is off**. `TabletTheme` always applies `TabletLightColorScheme` or `TabletDarkColorScheme`. No wallpaper palette.

## Rows, cards, jelly pill, scroll-hide

- Mail rows are flat on the background: no card, no border, no shadow (`MailRowLayout` in `ui/mail/ThreadRow.kt`). Minimum height is `MoSpacing.rowMinHeight` = **88dp**. Avatar is 40dp. Unread is bold sender/subject and primary-colored time.
- Cards are for grouped content. `MoCardShape` and `MoRadii.extraLarge` / `MoShapes.extraLarge` are **28dp**. `TabletShapes.extraLarge` is the same 28dp.
- The selected tab indicator is one **jelly pill** (`JellyBubble`): a single bubble slides between the five items (squash-and-stretch springs). Size in the bar is 64×32dp (half-width 32dp, height 32dp), 12dp from the top of the 80dp bar, `secondaryContainer` fill. `TabletPillShape` is the pill token.
- Scroll-hide is a pure translate, **250ms**. Hide uses `LinearOutSlowInEasing`. Show uses `FastOutSlowInEasing` (`AppNavGraph.kt` `barsTransition`). No fade on the bar slide.

## Large-screen layout target

Gmail large-screen behavior is the layout reference only. Do not copy Google branding, artwork, or Chat/Meet.

- Landscape / wide: a left navigation rail replaces the bottom bar. The list pane and the conversation pane sit side by side. A drag divider resizes them or collapses the workspace to one pane (Gmail 2025.04).
- Portrait / narrow: the five-destination bottom bar returns. List then conversation, not two persistent panes.
- Width classes decide this, not a device-name check. Mailstack destinations stay Home, Mail, Categories, Companies, Actions.

`TabletActivity` only hosts `setContent` → `TabletTheme` → `com.greninjaop.mailstack.tablet.shell.TabletShell()`. Agent 2 owns `TabletShell.kt`. This phase does not create shell or mail files.

## Module boundary

`AndroidTablet/` is its own Gradle root (`com.android.application` 8.13.2, Kotlin 2.3.21, Compose BOM `2026.06.01`, Material 3, minSdk 26, compileSdk/targetSdk 35). Dependencies are AndroidX core, Activity Compose, Compose UI, and Material 3. Room, KSP, DataStore, browser/OAuth, and Gmail client libraries are absent on purpose.
