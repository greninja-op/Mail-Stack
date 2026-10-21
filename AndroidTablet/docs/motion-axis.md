# Tablet motion axis

Phone motions this API keeps, taken from `Motion.kt` and `JellyBubble.kt` (bar timing from the phone navigation graph):

- **250ms bar translate.** Scroll-hide is a pure translate. Hiding uses LinearOutSlowIn. Showing uses FastOutSlowIn.
- **350ms settings slide.** FastOutSlowIn. On the phone the panel slides in from the right and out to the right. The screen underneath shifts with it.
- **Tab content slides from the side of the destination.** 350ms FastOutSlowIn, fully opaque (no fade, no scale). A tab to the right enters from the right. A tab to the left enters from the left.
- **Jelly pill squash.** The pill stretches along its travel (leading edge stiffer, trailing edge lagging) and flattens on the cross axis, up to 20% flatter, returning to 1 at rest. On the phone, travel is X and squash is `scaleY`.

`BarAnimMs` is 250. `SettingsAnimMs` is 350. Tab content uses the settings duration so the page and the pill still land together.

## Same gesture, other axis

The caller passes `ChromeAxis.Bottom` or `ChromeAxis.Leading`. When the user rotates, or the window crosses **600dp**, the caller swaps that value. The gesture does not change. It plays on the other axis.

| Gesture | Bottom bar (under 600dp) | Leading rail (600dp and wider) |
| --- | --- | --- |
| Hide chrome | Moves down (+Y) by the bar height | Moves off the leading edge (-X) by the rail width |
| Show chrome | Moves back up to rest | Moves back in from the leading edge |
| Pill travel | Eases along X | Eases along Y |
| Pill squash | `scaleY` | `scaleX` |
| Further destination | To the right; content enters from the right | Lower on the rail; content enters from the bottom |
| Nearer destination | To the left; content enters from the left | Higher on the rail; content enters from the top |

Hiding a bottom bar moves it down. Hiding a rail moves it off the leading edge. Those translations are opposites: for the same hidden fraction and span, the bottom offset's Y equals the negation of the rail offset's X.

`hiddenFraction` is 0 when the chrome is fully shown and 1 when it is fully hidden. A phone visibility fraction of 1 (shown) is a hidden fraction of 0.

Content for a destination to the right enters from the right. A destination lower on the rail enters from the bottom. Outgoing content leaves in the opposite direction. Settings keeps the 350ms FastOutSlowIn slide.

## Reduced motion

`rememberReducedMotion()` is true when `Settings.Global.ANIMATOR_DURATION_SCALE` is 0 (Remove animations, or animator duration scale off). It updates if that setting changes. If the setting cannot be read, motion stays on.

Reduced motion uses **snap**. Bar translate, the settings slide, and tab content all snap to the end state. Offsets are unchanged; only the duration goes away.

## API

```kotlin
enum class ChromeAxis { Bottom, Leading }

enum class PillTravel { X, Y }

const val BarAnimMs: Int = 250
const val SettingsAnimMs: Int = 350

fun axisOffset(axis: ChromeAxis, hiddenFraction: Float, spanPx: Float): IntOffset

fun pillTravel(axis: ChromeAxis): PillTravel

fun pillEdgeOffset(axis: ChromeAxis, alongPx: Float, crossPx: Float = 0f): IntOffset

fun pillSquashScale(stretchedPx: Float, restPx: Float): Float

fun contentEnterOffset(axis: ChromeAxis, destinationIsFurther: Boolean, spanPx: Int): IntOffset

fun contentExitOffset(axis: ChromeAxis, destinationIsFurther: Boolean, spanPx: Int): IntOffset

fun <T> barTranslateSpec(reducedMotion: Boolean, hiding: Boolean): FiniteAnimationSpec<T>

fun <T> settingsSlideSpec(reducedMotion: Boolean): FiniteAnimationSpec<T>

@Composable
fun rememberReducedMotion(): Boolean
```

This contract was not run on a tablet, an emulator, or a device.
