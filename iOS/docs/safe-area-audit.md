# iOS safe-area audit (from Android)

Android source of truth: `MoNavBar.kt` and `PrimaryContent` in `AppNavGraph.kt`. There was no iOS app in the repo.

## What Android does

- Edge-to-edge is on (`enableEdgeToEdge()`). Screen scaffolds set `contentWindowInsets` to zero so they do not add a second inset.
- The bottom bar is **80dp of content**, then `navigationBarsPadding()` **outside** that height. Total height is `80dp + live navigation-bar inset`.
- A bug (2026-10-16) put the padding inside the 80dp, which squeezed the icons onto the system buttons. The fix keeps the padding outside.
- List padding is `navigationBar inset + 80dp * barsOffset`.
- A second bug (2026-10-17) multiplied the system inset by `barsOffset`. When the app bar hid, the last row slid under the system buttons. The system inset is now **always** reserved. Only the 80dp app bar collapses.

Gesture navigation and fullscreen (inset 0) therefore leave **no empty slot** where the 3-button bar used to be. 3-button navigation (inset about 48dp) pads by that measured inset only.

## iPhone mapping

iPhone has no Android navigation buttons. The matching inset is the window's bottom safe area:

| Device / mode | Bottom inset | App bar | List padding while bars are visible |
| --- | --- | --- | --- |
| Home indicator (Face ID) | ~34pt | 80 + 34 | 34 + 80 |
| No home indicator (SE, or indicator hidden) | 0 | 80 | 80 |
| App bar scrolled away, indicator showing | ~34pt | off screen | 34 |
| App bar scrolled away, inset 0 | 0 | off screen | 0 |

`ChromeLayout` implements that math. `RootShell` reads `UIWindow.safeAreaInsets` on appear and when the scene becomes active, so a change in the live inset updates the padding. Nothing in the shell adds a constant 34, 48, or 80 gap on top of the measured inset.

The top bar is the same idea: 64pt content plus the status-bar inset, and that whole top padding collapses with scroll-hide.
