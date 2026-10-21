package com.greninjaop.mailstack.tablet.motion

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Edge the navigation chrome occupies.
 *
 * The phone keeps a bottom bar. At a wide window the same chrome is a
 * leading rail. Rotation or a window crossing 600dp swaps which value
 * the caller passes; the gesture stays the same and plays on the other axis.
 */
enum class ChromeAxis {
    /** Bottom navigation bar. Hiding translates down (+Y). */
    Bottom,

    /** Leading navigation rail. Hiding translates off the leading edge (-X). */
    Leading,
}

/**
 * Axis the jelly pill eases along.
 *
 * On a bottom bar the destinations sit in a row, so the pill travels on X
 * and squashes on Y (phone [scaleY]). On a rail the destinations stack, so
 * the pill travels on Y and squashes on X.
 */
enum class PillTravel {
    X,
    Y,
}

/**
 * Scroll-hide of the bar or rail. Phone bar translate is 250ms:
 * hide uses LinearOutSlowIn, show uses FastOutSlowIn.
 */
const val BarAnimMs: Int = 250

/**
 * Settings panel slide and primary-tab content slide. Phone uses 350ms
 * FastOutSlowIn for both, so they land together.
 */
const val SettingsAnimMs: Int = 350

/**
 * Translation that hides chrome.
 *
 * [hiddenFraction] is 0 when fully shown and 1 when fully hidden.
 * [spanPx] is the bar height ([ChromeAxis.Bottom]) or the rail width
 * ([ChromeAxis.Leading]).
 *
 * Bottom moves down (+Y). Leading moves off the leading edge (-X).
 * For the same fraction and span the two results are opposite:
 * `bottom.y == -leading.x`, and the other component is 0.
 */
fun axisOffset(axis: ChromeAxis, hiddenFraction: Float, spanPx: Float): IntOffset {
    if (hiddenFraction.isNaN() || spanPx.isNaN()) return IntOffset.Zero
    val distance = (hiddenFraction.coerceIn(0f, 1f) * abs(spanPx)).roundToInt()
    return when (axis) {
        ChromeAxis.Bottom -> IntOffset(0, distance)
        ChromeAxis.Leading -> IntOffset(-distance, 0)
    }
}

/** Pill travel for [axis]. Bottom eases on X. A rail eases on Y. */
fun pillTravel(axis: ChromeAxis): PillTravel = when (axis) {
    ChromeAxis.Bottom -> PillTravel.X
    ChromeAxis.Leading -> PillTravel.Y
}

/**
 * Places the pill's leading edge. [alongPx] moves along [pillTravel];
 * [crossPx] is the inset on the other axis (phone top offset).
 */
fun pillEdgeOffset(axis: ChromeAxis, alongPx: Float, crossPx: Float = 0f): IntOffset {
    val along = if (alongPx.isNaN()) 0 else alongPx.roundToInt()
    val cross = if (crossPx.isNaN()) 0 else crossPx.roundToInt()
    return when (pillTravel(axis)) {
        PillTravel.X -> IntOffset(along, cross)
        PillTravel.Y -> IntOffset(cross, along)
    }
}

/**
 * Cross-axis squash while the pill is stretched along [pillTravel].
 * Same volume rule as the phone pill: up to 20% flatter, 1 at rest.
 * Apply as `scaleY` when travel is X, and as `scaleX` when travel is Y.
 */
fun pillSquashScale(stretchedPx: Float, restPx: Float): Float {
    if (restPx <= 0f || stretchedPx.isNaN() || restPx.isNaN()) return 1f
    val stretch = ((stretchedPx - restPx) / restPx).coerceIn(0f, 1f)
    return 1f - stretch * 0.20f
}

/**
 * Where incoming destination content starts.
 *
 * [destinationIsFurther] is true when the destination sits later in the
 * destination list. On a bottom bar that is to the right, and the content
 * enters from the right (+X). On a rail that is lower, and the content
 * enters from the bottom (+Y). A destination the other way enters from
 * the opposite side. [spanPx] is the content width (bottom) or height (rail).
 */
fun contentEnterOffset(
    axis: ChromeAxis,
    destinationIsFurther: Boolean,
    spanPx: Int,
): IntOffset {
    val magnitude = abs(spanPx)
    val signed = if (destinationIsFurther) magnitude else -magnitude
    return when (axis) {
        ChromeAxis.Bottom -> IntOffset(signed, 0)
        ChromeAxis.Leading -> IntOffset(0, signed)
    }
}

/**
 * Where outgoing destination content leaves. Opposite of [contentEnterOffset]
 * for the same destination, matching the phone tab slide.
 */
fun contentExitOffset(
    axis: ChromeAxis,
    destinationIsFurther: Boolean,
    spanPx: Int,
): IntOffset {
    val enter = contentEnterOffset(axis, destinationIsFurther, spanPx)
    return IntOffset(-enter.x, -enter.y)
}

/**
 * Bar or rail translate. [hiding] selects the phone hide easing
 * (LinearOutSlowIn); showing uses FastOutSlowIn. Reduced motion snaps.
 */
fun <T> barTranslateSpec(reducedMotion: Boolean, hiding: Boolean): FiniteAnimationSpec<T> =
    if (reducedMotion) {
        snap()
    } else {
        tween(
            durationMillis = BarAnimMs,
            easing = if (hiding) LinearOutSlowInEasing else FastOutSlowInEasing,
        )
    }

/**
 * Settings slide and tab-content slide. 350ms FastOutSlowIn, or a snap
 * when [reducedMotion] is true. Tab content stays opaque (no fade, no scale).
 */
fun <T> settingsSlideSpec(reducedMotion: Boolean): FiniteAnimationSpec<T> =
    if (reducedMotion) {
        snap()
    } else {
        tween(durationMillis = SettingsAnimMs, easing = FastOutSlowInEasing)
    }

/**
 * True when the system animator duration scale is 0 (Remove animations,
 * or Developer options → Animator duration scale off).
 *
 * Reads [Settings.Global.ANIMATOR_DURATION_SCALE] and watches that setting.
 * If the read or the observer cannot be registered, returns false so motion
 * stays on.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember(resolver) { mutableStateOf(animatorScaleIsZero(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = animatorScaleIsZero(resolver)
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        runCatching { resolver.registerContentObserver(uri, false, observer) }
        reduced = animatorScaleIsZero(resolver)
        onDispose {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }
    return reduced
}

private fun animatorScaleIsZero(resolver: android.content.ContentResolver): Boolean =
    runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) <= 0f
    }.getOrDefault(false)
