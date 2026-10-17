package com.greninjaop.mailorganizer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Material scrollbar for a LazyColumn (2026-10-17).
 *
 * Behaviour (the user's spec, exactly):
 * - The scrollbar is INVISIBLE until the user scrolls. The moment a
 *   scroll starts it slides in from the right edge — no appear delay.
 * - When the page goes static it waits [HIDE_DELAY_MS], then slides
 *   back out to the right and fades. Static = no scrollbar at all.
 * - The slide is a translate-X + fade (FastOutSlowIn in, LinearOutSlowIn
 *   out), so it reads as popping out of / into the right edge.
 * - Press-and-hold on the thumb makes the THUMB grow (4dp -> 12dp, a
 *   spring pop). The track never grows — it stays a constant 2dp line.
 * - Dragging the thumb fast-scrolls the list (press on the track jumps
 *   the thumb to the press, then drags). The list behind never also
 *   scrolls: the down is consumed by the scrollbar's touch strip.
 *
 * The thumb's size/position are computed from [LazyListState.layoutInfo]
 * (first visible index + offset, average item height), so they track the
 * real scroll position. A list that cannot scroll (content fits) shows
 * no scrollbar at all.
 */
@Composable
fun MaterialScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var visible by remember { mutableStateOf(false) }
    var thumbPressed by remember { mutableStateOf(false) }
    var hideJob by remember { mutableStateOf<Job?>(null) }

    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    val canScroll = listState.canScrollBackward || listState.canScrollForward

    // Never show a scrollbar for a list that cannot scroll.
    LaunchedEffect(canScroll) {
        if (!canScroll) visible = false
    }

    // Show on any scroll activity; hide 1.5s after the last one.
    // drop(1) skips snapshotFlow's immediate initial emission, so the
    // scrollbar does not appear on first composition before any scroll.
    LaunchedEffect(listState) {
        merge(
            snapshotFlow { listState.isScrollInProgress }.drop(1),
            snapshotFlow {
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }.drop(1),
        ).collect {
            if (!listState.canScrollBackward && !listState.canScrollForward) return@collect
            visible = true
            hideJob?.cancel()
            hideJob = scope.launch {
                delay(HIDE_DELAY_MS)
                if (!listState.isScrollInProgress && !thumbPressed) visible = false
            }
        }
    }

    // Visibility 0..1: drives both the fade and the slide from the right.
    val visibility = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            visibility.animateTo(1f, tween(APPEAR_MS, easing = FastOutSlowInEasing))
        } else {
            visibility.animateTo(0f, tween(DISAPPEAR_MS, easing = LinearOutSlowInEasing))
        }
    }

    // Thumb width: the thumb (the "wheel" under the finger) grows on
    // press; the track never changes width.
    val thumbWidth by animateDpAsState(
        targetValue = if (thumbPressed) PRESSED_THUMB_WIDTH else THUMB_WIDTH,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "scrollbarThumbWidth",
    )

    if (totalItems == 0 || visibleItems.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(TOUCH_WIDTH)
            // Slide in from / out to the right edge + fade, driven by
            // the visibility animation. At 0 the strip sits fully off
            // the right edge (invisible); at 1 it is flush on screen.
            .graphicsLayer {
                alpha = visibility.value
                translationX = (1f - visibility.value) * (TOUCH_WIDTH.toPx() + 4.dp.toPx())
            }
            .then(
                if (visible) {
                    // Two detectors on the same strip (they run in
                    // parallel, neither blocks the other):
                    // 1. Press: the thumb grows while the finger is down
                    //    (press-and-hold), even before any movement.
                    // 2. Drag: fast-scroll the list. A drag that starts on
                    //    the track (not the thumb) jumps the thumb to the
                    //    press first, then drags from there.
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    thumbPressed = true
                                    try {
                                        awaitRelease()
                                    } finally {
                                        thumbPressed = false
                                    }
                                },
                            )
                        }
                        .pointerInput(listState) {
                            detectDragGestures(
                                onDragStart = { downOffset ->
                                    val trackHeightPx = size.height.toFloat()
                                    val geometry = thumbGeometry(
                                        layoutInfo = listState.layoutInfo,
                                        trackHeightPx = trackHeightPx,
                                        density = density.density,
                                    ) ?: return@detectDragGestures
                                    val downY = downOffset.y
                                    val onThumb = downY in geometry.top..(geometry.top + geometry.height)
                                    if (!onThumb) {
                                        val fraction = (downY / trackHeightPx).coerceIn(0f, 1f)
                                        val targetScroll = fraction * geometry.totalHeight -
                                            geometry.viewportHeight / 2f
                                        val avg = geometry.avgItemHeight
                                        val index = (targetScroll / avg).toInt()
                                            .coerceIn(0, (totalItems - 1).coerceAtLeast(0))
                                        val offset = -(targetScroll - index * avg).toInt()
                                        scope.launch { listState.scrollToItem(index, offset) }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    // Consume: the list behind must not
                                    // also scroll from this touch.
                                    change.consume()
                                    val trackHeightPx = size.height.toFloat()
                                    val geometry = thumbGeometry(
                                        layoutInfo = listState.layoutInfo,
                                        trackHeightPx = trackHeightPx,
                                        density = density.density,
                                    ) ?: return@detectDragGestures
                                    val fraction = dragAmount.y / trackHeightPx
                                    scope.launch {
                                        listState.scrollBy(fraction * geometry.totalHeight)
                                    }
                                },
                            )
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        val trackHeightPx = constraints.maxHeight.toFloat()
        val geometry = thumbGeometry(
            layoutInfo = layoutInfo,
            trackHeightPx = trackHeightPx,
            density = density.density,
        ) ?: return@BoxWithConstraints

        // Track: constant 2dp, never grows.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(TRACK_WIDTH)
                .fillMaxHeight()
                .clip(RoundedCornerShape(TRACK_WIDTH / 2))
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                ),
        )
        // Thumb: the "wheel". Grows on press (thumbWidth animates);
        // the track above stays exactly the same width.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, geometry.top.toInt()) }
                .width(thumbWidth)
                .height(with(density) { geometry.height.toDp() })
                .clip(RoundedCornerShape(thumbWidth / 2))
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                ),
        )
    }
}

/** Thumb geometry in px, from the list's layout info. Null when the list cannot scroll. */
private data class ThumbGeometry(
    val top: Float,
    val height: Float,
    val totalHeight: Float,
    val viewportHeight: Float,
    val avgItemHeight: Float,
)

private fun thumbGeometry(
    layoutInfo: androidx.compose.foundation.lazy.LazyListLayoutInfo,
    trackHeightPx: Float,
    density: Float,
): ThumbGeometry? {
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems == 0 || visibleItems.isEmpty()) return null
    val avgItemHeight = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
    if (avgItemHeight <= 0f) return null
    val totalHeight = avgItemHeight * totalItems
    val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
    if (totalHeight <= viewportHeight) return null // Content fits: no scrollbar.
    val first = visibleItems.first()
    // Scroll offset: items above + how far the first visible item is
    // scrolled off the top (its offset goes negative as it leaves).
    val scrollOffset = (first.index * avgItemHeight - first.offset).coerceAtLeast(0f)
    val minThumbPx = MIN_THUMB_HEIGHT.value * density
    val thumbHeight = (viewportHeight / totalHeight * trackHeightPx).coerceIn(minThumbPx, trackHeightPx)
    val thumbTop = (scrollOffset / totalHeight * trackHeightPx).coerceIn(0f, trackHeightPx - thumbHeight)
    return ThumbGeometry(
        top = thumbTop,
        height = thumbHeight,
        totalHeight = totalHeight,
        viewportHeight = viewportHeight,
        avgItemHeight = avgItemHeight,
    )
}

private const val HIDE_DELAY_MS = 1500L
private const val APPEAR_MS = 200
private const val DISAPPEAR_MS = 250
private val TOUCH_WIDTH = 24.dp
private val TRACK_WIDTH = 2.dp
private val THUMB_WIDTH = 4.dp
private val PRESSED_THUMB_WIDTH = 12.dp
private val MIN_THUMB_HEIGHT = 24.dp
