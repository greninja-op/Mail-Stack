# iPad motion and insets

Gmail on iPad shows the conversation list and the open message side by side in landscape, collapses to a single column in portrait and whenever the window is narrow, and keeps labels in a side menu. Mailstack uses that structure: a wide window shows the list and the selected message together, and a portrait or narrow window (including a tight Split View or Slide Over) shows one column. The interface is Mailstack's own violet UI, and the sidebar holds the five product sections — Home, Mail, Categories, Companies, and Actions — rather than a Gmail label list.

`PadMotion.swift` is the motion contract for that chrome. It keeps the phone and Android timings and flips the axis the chrome travels on.

## Durations

| Motion | Duration | Phone | Android |
| --- | --- | --- | --- |
| Bar hide / show | 0.25s | `AppModel.setBars`, `easeInOut` | Bars tween, 250ms. Hide is LinearOutSlowIn, show is FastOutSlowIn. Pure translate, no fade. |
| Settings slide | 0.35s | `RootShell` settings transition, `easeInOut` | `MoMotion.panelSlide`, 350ms FastOutSlowIn. The panel slides in from the trailing edge. |

`PadMotion.barHide` and `PadMotion.settingsSlide` are those two `easeInOut` curves. Entering a section shows the chrome again, same as entering a phone tab. When Reduce Motion is on, use `Animation.none`, the same branch `AppModel.setBars` and `RootShell` already take.

## Chrome axis

Phone and Android hide the bottom bar by translating it down:

```text
offset.y = (1 - barsShown) * barHeight
```

That is a positive height. iPad's wide chrome is a leading sidebar, so the same hide travels the other axis, and the sign flips with it. `padHideOffset(axis:hidden:span:)`:

| `PadChromeAxis` | Result | Meaning |
| --- | --- | --- |
| `.bottom` | width `0`, height `+span * hidden` | Slides down, off the bottom edge. Used for a bottom bar, including the narrow-width phone fallback. |
| `.leading` | width `-span * hidden`, height `0` | Slides off the left. Used for the sidebar and the navigation rail. |

These two translations are opposites: down is positive height, off the left is negative width. `hidden` is `0` when the chrome is fully shown and `1` when it is fully off screen. It is the complement of the phone `barsAmount` and the Android `barsOffset` (those values are `1` when the bars are shown). `span` is that chrome's own height or width — how far it must move to leave the screen.

## Content edge

Phone and Android primary tabs are a horizontal row. The incoming screen enters from the side that tab sits on: a tab on the right enters from the trailing edge, a tab on the left from the leading edge.

iPad flips that axis. The five sections are ordered top to bottom, and `padContentEdge(forDestinationIndex:count:)` picks the vertical edge the destination occupies:

- Top half (`index < count / 2`) enters from `.top`.
- Bottom half (`index >= count / 2`) enters from `.bottom`.

Integer division truncates, so an odd count puts the middle index in the bottom half. For the five product sections (`count` 5, `count / 2` is 2):

| Index | Section | Half | Enters from |
| --- | --- | --- | --- |
| 0 | Home | top | `.top` |
| 1 | Mail | top | `.top` |
| 2 | Categories | bottom | `.bottom` |
| 3 | Companies | bottom | `.bottom` |
| 4 | Actions | bottom | `.bottom` |

An even count splits evenly: the first half from the top, the second half from the bottom. A `count` of 0 or less has no top half, and the function returns `.bottom`.

## Safe area

Portrait, landscape, Split View, and Slide Over are different windows. Each one has its own bottom safe area, and that value changes when the divider moves, the scene becomes active, or the window rotates. Read the live bottom inset from the current window (`UIWindow.safeAreaInsets.bottom`, or the SwiftUI safe area of that scene). Read it again when the size or scene phase changes. Do not cache a number from launch.

An inset of 0 means the content sits on the edge. There is no gap to reserve. A non-zero inset is whatever the window reports at that moment, and only that measured amount is reserved. Hiding chrome collapses the chrome's own span. It does not erase a live system inset, and it does not invent one when the window reports 0. That is the same rule as phone `ChromeLayout`: the system inset stays when the app bar hides, and a 0 inset leaves the last row on the screen edge.

Never reserve a constant 34pt hole or a constant 49pt hole. 34pt is the iPhone home indicator and 49pt is the iPhone tab bar. Neither bar is on the iPad screen. A hardcoded hole of either size leaves an empty band under the content in portrait, in landscape, in Split View, and in Slide Over. The leading sidebar does not change this: its hide offset is horizontal, and the bottom inset is still the live measurement, or 0 when the window has none.
