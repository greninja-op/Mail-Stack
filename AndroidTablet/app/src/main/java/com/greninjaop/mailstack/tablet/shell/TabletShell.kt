package com.greninjaop.mailstack.tablet.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.greninjaop.mailstack.tablet.motion.BarAnimMs
import com.greninjaop.mailstack.tablet.motion.ChromeAxis
import com.greninjaop.mailstack.tablet.motion.axisOffset
import com.greninjaop.mailstack.tablet.motion.barTranslateSpec
import com.greninjaop.mailstack.tablet.motion.contentEnterOffset
import com.greninjaop.mailstack.tablet.motion.rememberReducedMotion
import com.greninjaop.mailstack.tablet.motion.settingsSlideSpec
import kotlin.math.abs

/**
 * Tablet root. Narrow windows keep the phone bottom bar. At 600dp the same
 * destinations move to a leading rail and mail becomes a list | detail pair
 * with a drag handle. Crossing the breakpoint slides the chrome on its new
 * axis instead of popping it into place.
 */
@Composable
fun TabletShell() {
    val reducedMotion = rememberReducedMotion()
    var selectedName by rememberSaveable { mutableStateOf(TabletDestination.Home.name) }
    val selected = TabletDestination.entries.firstOrNull { it.name == selectedName }
        ?: TabletDestination.Home
    var listFraction by rememberSaveable { mutableFloatStateOf(0.4f) }
    var showList by rememberSaveable { mutableStateOf(true) }
    var showDetail by rememberSaveable { mutableStateOf(true) }
    var overscrollPx by remember { mutableFloatStateOf(0f) }
    var barsVisible by remember { mutableStateOf(true) }

    val barsTransition = updateTransition(barsVisible, label = "bars")
    val barsOffset by barsTransition.animateFloat(
        transitionSpec = { barTranslateSpec(reducedMotion, hiding = !targetState) },
        label = "bars-offset",
    ) { visible -> if (visible) 1f else 0f }

    val barScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) barsVisible = false
                else if (available.y > 0f) barsVisible = true
                return Offset.Zero
            }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val wide = maxWidth >= 600.dp
        val axisFraction by animateFloatAsState(
            targetValue = if (wide) 1f else 0f,
            animationSpec = if (reducedMotion) {
                snap()
            } else {
                tween(durationMillis = BarAnimMs, easing = FastOutSlowInEasing)
            },
            label = "chrome-axis",
        )
        // Narrow: bottom padding is the system nav inset plus 80dp times the
        // scroll-hide offset. The inset is never multiplied by barsOffset.
        // Wide: the 80dp term is gone, so nothing is left where the bar was.
        val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val barShown = barsOffset * (1f - axisFraction)
        val railShown = barsOffset * axisFraction
        val bottomPad = navBar + TabletBarHeight * barShown
        val startPad = TabletRailWidth * railShown
        val density = LocalDensity.current
        val bottomSpanPx = with(density) { (TabletBarHeight + navBar).toPx() }
        val railSpanPx = with(density) { TabletRailWidth.toPx() }
        val axis = if (wide) ChromeAxis.Leading else ChromeAxis.Bottom

        LaunchedEffect(selected, wide) { barsVisible = true }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(start = startPad, top = statusBar, bottom = bottomPad)
                .consumeWindowInsets(PaddingValues(start = startPad, top = statusBar, bottom = bottomPad))
                .nestedScroll(barScrollConnection)
                .clipToBounds(),
        ) {
            val spanPx = if (axis == ChromeAxis.Bottom) constraints.maxWidth else constraints.maxHeight
            TabletDestination.entries.forEach { destination ->
                DestinationLayer(
                    destination = destination,
                    selected = selected,
                    axis = axis,
                    spanPx = spanPx,
                    reducedMotion = reducedMotion,
                ) {
                    TabletDestinationContent(
                        destination = destination,
                        wide = wide,
                        listFraction = listFraction,
                        showList = showList,
                        showDetail = showDetail,
                    )
                    if (wide && destination == TabletDestination.Mail) {
                        val collapseSlop = with(density) { SplitCollapseSlop.toPx() }
                        val reopenSlop = with(density) { SplitReopenSlop.toPx() }
                        TabletPaneDivider(
                            split = TabletSplit(listFraction, showList, showDetail, overscrollPx),
                            onDragDelta = { deltaPx, widthPx ->
                                val next = TabletSplit(
                                    listFraction,
                                    showList,
                                    showDetail,
                                    overscrollPx,
                                ).dragged(deltaPx, widthPx, collapseSlop, reopenSlop)
                                listFraction = next.listFraction
                                showList = next.showList
                                showDetail = next.showDetail
                                overscrollPx = next.overscrollPx
                            },
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
        }

        TabletDestinationChrome(
            axis = ChromeAxis.Bottom,
            selected = selected,
            onSelect = { selectedName = it.name },
            reducedMotion = reducedMotion,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(2f)
                .offset { axisOffset(ChromeAxis.Bottom, 1f - barShown, bottomSpanPx) }
                .then(
                    if (axisFraction > 0.5f) Modifier.clearAndSetSemantics {} else Modifier,
                ),
        )
        TabletDestinationChrome(
            axis = ChromeAxis.Leading,
            selected = selected,
            onSelect = { selectedName = it.name },
            reducedMotion = reducedMotion,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(2f)
                .offset { axisOffset(ChromeAxis.Leading, 1f - railShown, railSpanPx) }
                .then(
                    if (axisFraction < 0.5f) Modifier.clearAndSetSemantics {} else Modifier,
                ),
        )
    }
}

/**
 * Keeps every destination composed so a tab change does not rebuild it.
 * The incoming page enters from the side it sits on: from the right on the
 * bar, from below on the rail. Reduced motion snaps.
 */
@Composable
private fun DestinationLayer(
    destination: TabletDestination,
    selected: TabletDestination,
    axis: ChromeAxis,
    spanPx: Int,
    reducedMotion: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val target = restingOffset(destination, selected, axis, spanPx)
    val anim = remember(destination) { Animatable(target, IntOffset.VectorConverter) }
    var previousSelected by remember(destination) { mutableStateOf(selected) }
    var previousAxis by remember(destination) { mutableStateOf(axis) }
    LaunchedEffect(selected, axis, spanPx, reducedMotion, target) {
        val selectionChanged = previousSelected != selected
        val involved = destination == selected || destination == previousSelected
        val axisChanged = previousAxis != axis
        previousSelected = selected
        previousAxis = axis
        val snapNow = reducedMotion || !selectionChanged || !involved || axisChanged || spanPx == 0
        if (snapNow) {
            anim.snapTo(target)
        } else {
            anim.animateTo(target, settingsSlideSpec(false))
        }
    }
    val hidden = destination != selected
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(if (hidden) 0f else 1f)
            .offset { anim.value }
            .then(
                if (hidden) {
                    Modifier
                        .clearAndSetSemantics {}
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

private fun restingOffset(
    destination: TabletDestination,
    selected: TabletDestination,
    axis: ChromeAxis,
    spanPx: Int,
): IntOffset {
    if (destination == selected) return IntOffset.Zero
    val magnitude = if (spanPx <= 0) 100_000 else abs(spanPx)
    return contentEnterOffset(
        axis = axis,
        destinationIsFurther = destination.ordinal > selected.ordinal,
        spanPx = magnitude,
    )
}
