package com.greninjaop.mailorganizer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The jelly bubble — one sliding pill with squash-and-stretch physics,
 * shared by every place that highlights a selection (bottom-nav tabs,
 * Appearance segmented control, ...).
 *
 * The motion follows the squash-and-stretch principle (Disney's 12
 * principles; the same physics as a toggle-switch ball): the pill
 * stretches along its direction of travel while the trailing edge lags
 * behind, then the trailing edge catches up and the pill settles with
 * a small spring wobble. Implementation: the pill's left and right
 * edges are two independent [Animatable]s — the leading edge runs a
 * stiffer spring (fast start) and the trailing edge a softer one
 * (lags, so the pill elongates mid-flight), both underdamped so the
 * pill overshoots slightly and shakes into place. Vertical squash is
 * derived from the stretch (volume conservation: wider → flatter).
 *
 * The caller owns the geometry (where the pill should centre, how wide
 * it should be); this component owns the motion. The caller reads
 * [JellyBubbleState.centerPx] to sync content with the pill — e.g.
 * tint an icon, or fade a checkmark in, the moment the bubble covers it.
 */
class JellyBubbleState internal constructor() {
    internal val leftEdge = Animatable(0f)
    internal val rightEdge = Animatable(0f)
    private var initialized = false

    /** Current centre of the bubble, in the container's coordinate space. */
    val centerPx: Float get() = (leftEdge.value + rightEdge.value) / 2f

    /** Current width of the bubble (stretched mid-flight, rest width at rest). */
    val widthPx: Float get() = rightEdge.value - leftEdge.value

    /** Vertical squash from the current stretch (1.0 at rest width). */
    fun squash(restWidthPx: Float): Float = pillSquashScaleY(widthPx, restWidthPx)

    /**
     * Move the bubble to a new target. First call snaps (the bubble
     * appears at the selection instead of animating in from nowhere);
     * later calls animate the two edges with the directional springs.
     * Safe to call on every recomposition — a no-op when already there.
     */
    internal suspend fun moveTo(targetCenterPx: Float, targetHalfWidthPx: Float) {
        if (targetCenterPx <= 0f) return
        val (targetLeft, targetRight) = pillTargetEdges(targetCenterPx, targetHalfWidthPx)
        if (!initialized) {
            leftEdge.snapTo(targetLeft)
            rightEdge.snapTo(targetRight)
            initialized = true
            return
        }
        if (leftEdge.value == targetLeft && rightEdge.value == targetRight) return
        val movingRight = targetCenterPx > centerPx
        // Two concurrent edge animations; the caller awaits both.
        coroutineScope {
            launch {
                leftEdge.animateTo(
                    targetLeft,
                    if (movingRight) trailingEdgeSpring() else leadingEdgeSpring(),
                )
            }
            launch {
                rightEdge.animateTo(
                    targetRight,
                    if (movingRight) leadingEdgeSpring() else trailingEdgeSpring(),
                )
            }
        }
    }
}

@Composable
fun rememberJellyBubbleState(): JellyBubbleState = remember { JellyBubbleState() }

/**
 * Draws the bubble for [state] at its current edges, driving
 * [JellyBubbleState.moveTo] toward the target. The bubble is drawn
 * with [color] (default: the Material 3 active-indicator colour,
 * secondaryContainer) and [height]; its width follows the animation.
 */
@Composable
fun JellyBubble(
    state: JellyBubbleState,
    targetCenterPx: Float,
    targetHalfWidthPx: Float,
    height: Dp,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    modifier: Modifier = Modifier,
    topOffset: Dp = 0.dp,
) {
    val density = LocalDensity.current
    val topPx = with(density) { topOffset.toPx() }

    LaunchedEffect(targetCenterPx, targetHalfWidthPx) {
        state.moveTo(targetCenterPx, targetHalfWidthPx)
    }

    val squash = state.squash(targetHalfWidthPx * 2f)
    Box(
        modifier
            .offset {
                IntOffset(state.leftEdge.value.roundToInt(), topPx.roundToInt())
            }
            .size(
                width = with(density) { state.widthPx.toDp() },
                height = height,
            )
            .graphicsLayer { scaleY = squash }
            .clip(RoundedCornerShape(percent = 50))
            .background(color),
    )
}

/**
 * Target left/right edges for a bubble centred at [centerPx].
 * Pure function, unit-tested.
 */
internal fun pillTargetEdges(centerPx: Float, halfWidthPx: Float): Pair<Float, Float> =
    (centerPx - halfWidthPx) to (centerPx + halfWidthPx)

/**
 * Leading-edge spring: stiff, so the bubble starts fast and the leading
 * edge races toward the target.
 */
internal fun leadingEdgeSpring(): SpringSpec<Float> =
    spring(dampingRatio = 0.70f, stiffness = 550f)

/**
 * Trailing-edge spring: softer, so the trailing edge lags behind —
 * that lag is what stretches the bubble mid-flight. Same damping, so
 * both edges settle with the same wobble.
 */
internal fun trailingEdgeSpring(): SpringSpec<Float> =
    spring(dampingRatio = 0.70f, stiffness = 300f)

/**
 * Vertical squash from the stretch (volume conservation): a wider bubble
 * gets flatter, and returns to 1.0 as it settles back to normal width.
 * [widthPx] is the bubble's current width, [normalWidthPx] its rest width.
 * Pure function, unit-tested.
 */
internal fun pillSquashScaleY(widthPx: Float, normalWidthPx: Float): Float {
    if (normalWidthPx <= 0f) return 1f
    val stretch = ((widthPx - normalWidthPx) / normalWidthPx).coerceIn(0f, 1f)
    return 1f - stretch * 0.20f
}

/**
 * True when the bubble covers a point (icon, checkmark, ...) — content
 * tints/fades in the moment the bubble arrives over it, so bubble +
 * content read as one unit. Pure function, unit-tested.
 */
internal fun pillCoversIcon(pillCenterPx: Float, iconCenterPx: Float, halfWidthPx: Float): Boolean =
    abs(pillCenterPx - iconCenterPx) < halfWidthPx

/**
 * Centres of [count] equal segments across a [widthPx]-wide container.
 * Pure function, unit-tested.
 */
internal fun segmentCenters(widthPx: Float, count: Int): List<Float> =
    List(count) { (it + 0.5f) * widthPx / count }

/**
 * Half-width of a bubble inside one segment: the segment minus the
 * inset on both sides, halved. Pure function, unit-tested.
 */
internal fun segmentPillHalfWidth(segmentWidthPx: Float, insetPx: Float): Float =
    (segmentWidthPx - insetPx * 2f) / 2f
