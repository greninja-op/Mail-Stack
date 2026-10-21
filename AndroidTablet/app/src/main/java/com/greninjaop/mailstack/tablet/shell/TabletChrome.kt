package com.greninjaop.mailstack.tablet.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.greninjaop.mailstack.tablet.motion.ChromeAxis
import com.greninjaop.mailstack.tablet.motion.PillTravel
import com.greninjaop.mailstack.tablet.motion.pillEdgeOffset
import com.greninjaop.mailstack.tablet.motion.pillSquashScale
import com.greninjaop.mailstack.tablet.motion.pillTravel
import com.greninjaop.mailstack.tablet.theme.TabletPillShape
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Phone NavigationBar height. The system nav inset sits outside this. */
val TabletBarHeight: Dp = 80.dp

/**
 * Leading rail width. Icons sit above labels, same five destinations as the bar.
 * Wide enough for "Categories" at the default label size; larger fonts ellipsize.
 */
val TabletRailWidth: Dp = 96.dp

/** Resting pill, same size on the bar and the rail. Travel axis changes, the shape does not. */
private val PillWidth = 64.dp
private val PillHeight = 32.dp

/** Phone places the pill 12dp from the top of the 80dp bar, over the icon. */
private val PillTopOffset = 12.dp

private val IconSize = 24.dp
private val IconSlot = 32.dp

/**
 * Open list/detail shares. [TabletMailWorkspace] clamps the fraction to this
 * range, so the handle must use the same stops or it leaves the seam.
 */
internal const val SplitFractionMin = 0.2f
internal const val SplitFractionMax = 0.8f

/** Extra pull past a stop before a pane collapses, so the seam does not flicker. */
internal val SplitCollapseSlop = 32.dp

/** Pull back from a collapsed edge before the hidden pane returns. */
internal val SplitReopenSlop = 48.dp

/**
 * List | detail split owned by the shell. The mail workspace only reads the flags.
 *
 * [listFraction] is the list's share while both panes are open (0.2..0.8).
 * [overscrollPx] is pull past the min or max stop; it is not a width.
 */
internal data class TabletSplit(
    val listFraction: Float = 0.4f,
    val showList: Boolean = true,
    val showDetail: Boolean = true,
    val overscrollPx: Float = 0f,
)

/**
 * Apply one horizontal drag step.
 *
 * [deltaPx] grows the list when positive. [widthPx] is the content width.
 * Dragging past either open stop accumulates overscroll and then collapses
 * that side: detail collapses to list-only, list collapses to detail-only.
 * Dragging back from the collapsed edge reopens at the near stop.
 */
internal fun TabletSplit.dragged(
    deltaPx: Float,
    widthPx: Float,
    collapseSlopPx: Float,
    reopenSlopPx: Float,
): TabletSplit {
    if (widthPx <= 1f || deltaPx.isNaN()) return this
    if (!showList && !showDetail) {
        return copy(showList = true, showDetail = true, listFraction = 0.4f, overscrollPx = 0f)
    }
    val minPx = SplitFractionMin * widthPx
    val maxPx = SplitFractionMax * widthPx
    val currentPx = when {
        showList && showDetail -> listFraction.coerceIn(SplitFractionMin, SplitFractionMax) * widthPx
        showList -> widthPx
        else -> 0f
    }

    if (!showList) {
        val pulled = (overscrollPx + deltaPx).coerceAtLeast(0f)
        if (pulled < reopenSlopPx) return copy(overscrollPx = pulled)
        val opened = minPx + (pulled - reopenSlopPx)
        val fraction = (opened / widthPx).coerceIn(SplitFractionMin, SplitFractionMax)
        return copy(listFraction = fraction, showList = true, showDetail = true, overscrollPx = 0f)
    }
    if (!showDetail) {
        val pulled = (overscrollPx - deltaPx).coerceAtLeast(0f)
        if (pulled < reopenSlopPx) return copy(overscrollPx = pulled)
        val openedFromEnd = minPx + (pulled - reopenSlopPx)
        val listPx = widthPx - openedFromEnd
        val fraction = (listPx / widthPx).coerceIn(SplitFractionMin, SplitFractionMax)
        return copy(listFraction = fraction, showList = true, showDetail = true, overscrollPx = 0f)
    }

    val desired = currentPx + deltaPx
    return when {
        desired < minPx -> {
            val pulled = overscrollPx + (minPx - desired)
            if (pulled >= collapseSlopPx) {
                copy(listFraction = SplitFractionMin, showList = false, showDetail = true, overscrollPx = 0f)
            } else {
                copy(listFraction = SplitFractionMin, overscrollPx = pulled)
            }
        }
        desired > maxPx -> {
            val pulled = overscrollPx + (desired - maxPx)
            if (pulled >= collapseSlopPx) {
                copy(listFraction = SplitFractionMax, showList = true, showDetail = false, overscrollPx = 0f)
            } else {
                copy(listFraction = SplitFractionMax, overscrollPx = pulled)
            }
        }
        else -> copy(
            listFraction = (desired / widthPx).coerceIn(SplitFractionMin, SplitFractionMax),
            showList = true,
            showDetail = true,
            overscrollPx = 0f,
        )
    }
}

/**
 * Bottom bar or leading rail. One secondaryContainer pill for the selection.
 * On [ChromeAxis.Bottom] the pill eases on X. On [ChromeAxis.Leading] the same
 * pill eases on Y. Reduced motion snaps.
 *
 * The bar's [navigationBarsPadding] is outside [TabletBarHeight].
 */
@Composable
internal fun TabletDestinationChrome(
    axis: ChromeAxis,
    selected: TabletDestination,
    onSelect: (TabletDestination) -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val destinations = TabletDestination.entries
    val selectedIndex = destinations.indexOf(selected).coerceAtLeast(0)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier,
    ) {
        when (axis) {
            ChromeAxis.Bottom -> Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(TabletBarHeight),
            ) {
                ChromeItems(
                    axis = axis,
                    destinations = destinations,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ChromeAxis.Leading -> Box(
                Modifier
                    .width(TabletRailWidth)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                ChromeItems(
                    axis = axis,
                    destinations = destinations,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ChromeItems(
    axis: ChromeAxis,
    destinations: List<TabletDestination>,
    selectedIndex: Int,
    onSelect: (TabletDestination) -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val travel = pillTravel(axis)
    val density = LocalDensity.current
    val restAlongPx = with(density) {
        (if (travel == PillTravel.X) PillWidth else PillHeight).toPx()
    }
    val restCrossPx = with(density) {
        (if (travel == PillTravel.X) PillHeight else PillWidth).toPx()
    }
    val pill = remember(axis) { SlidingPillState() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val iconCenters = remember(axis) {
        mutableStateListOf<Offset>().apply {
            repeat(destinations.size) { add(Offset.Zero) }
        }
    }
    var centersReady by remember(axis) { mutableStateOf(false) }

    val target = iconCenters.getOrNull(selectedIndex) ?: Offset.Zero
    val targetAlong = if (travel == PillTravel.X) target.x else target.y
    val targetCross = if (travel == PillTravel.X) target.y else target.x
    LaunchedEffect(targetAlong, restAlongPx, reducedMotion, centersReady) {
        if (!centersReady) return@LaunchedEffect
        pill.moveTo(targetAlong, restAlongPx / 2f, reducedMotion)
    }

    Box(
        modifier.onPlaced { origin = it.positionInRoot() },
    ) {
        val squash = pillSquashScale(pill.length, restAlongPx)
        val lengthDp = with(density) { pill.length.toDp() }
        if (pill.placed) {
            Box(
                Modifier
                    .offset {
                        pillEdgeOffset(
                            axis,
                            pill.leading,
                            targetCross - restCrossPx / 2f,
                        )
                    }
                    .size(
                        width = if (travel == PillTravel.X) lengthDp else PillWidth,
                        height = if (travel == PillTravel.X) PillHeight else lengthDp,
                    )
                    .graphicsLayer {
                        if (travel == PillTravel.X) scaleY = squash else scaleX = squash
                    }
                    .clip(TabletPillShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
        }
        if (axis == ChromeAxis.Bottom) {
            Row(Modifier.fillMaxSize()) {
                destinations.forEachIndexed { index, dest ->
                    DestinationItem(
                        destination = dest,
                        selected = index == selectedIndex,
                        covered = pillCovers(
                            pill.center,
                            iconCenters[index].x,
                            restAlongPx / 2f,
                        ),
                        onClick = { if (index != selectedIndex) onSelect(dest) },
                        onIconCenter = { center ->
                            val previous = iconCenters[index]
                            if (abs(previous.x - center.x) > 0.5f || abs(previous.y - center.y) > 0.5f) {
                                iconCenters[index] = center
                            }
                            if (index == selectedIndex && !centersReady) centersReady = true
                        },
                        origin = { origin },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        iconTopSpacer = PillTopOffset,
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                destinations.forEachIndexed { index, dest ->
                    DestinationItem(
                        destination = dest,
                        selected = index == selectedIndex,
                        covered = pillCovers(
                            pill.center,
                            iconCenters[index].y,
                            restAlongPx / 2f,
                        ),
                        onClick = { if (index != selectedIndex) onSelect(dest) },
                        onIconCenter = { center ->
                            val previous = iconCenters[index]
                            if (abs(previous.x - center.x) > 0.5f || abs(previous.y - center.y) > 0.5f) {
                                iconCenters[index] = center
                            }
                            if (index == selectedIndex && !centersReady) centersReady = true
                        },
                        origin = { origin },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp),
                        iconTopSpacer = 4.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationItem(
    destination: TabletDestination,
    selected: Boolean,
    covered: Boolean,
    onClick: () -> Unit,
    onIconCenter: (Offset) -> Unit,
    origin: () -> Offset,
    modifier: Modifier = Modifier,
    iconTopSpacer: Dp,
) {
    val interaction = remember(destination) { MutableInteractionSource() }
    val tabSelected = selected
    Column(
        modifier
            .clickable(
                role = Role.Tab,
                indication = null,
                interactionSource = interaction,
                onClick = onClick,
            )
            .semantics {
                selected = tabSelected
                contentDescription = "${destination.label} tab"
            }
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(iconTopSpacer))
        Box(
            Modifier.size(IconSlot),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(IconSize)
                    .onPlaced { coords ->
                        val topLeft = coords.positionInRoot() - origin()
                        onIconCenter(
                            topLeft + Offset(
                                coords.size.width / 2f,
                                coords.size.height / 2f,
                            ),
                        )
                    },
                tint = if (covered) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Drag handle between the mail list and the conversation.
 * Solid rule and grip — no blur, no translucency.
 * The handle is only as wide as its touch target, so the panes keep receiving taps.
 */
@Composable
internal fun TabletPaneDivider(
    split: TabletSplit,
    onDragDelta: (deltaPx: Float, widthPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drag = rememberUpdatedState(onDragDelta)
    val layoutDirection = LocalLayoutDirection.current
    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val touch = 48.dp
        val seam = when {
            split.showList && split.showDetail -> maxWidth * split.listFraction
            split.showList -> maxWidth
            else -> 0.dp
        }
        val maxX = (maxWidth - touch).coerceAtLeast(0.dp)
        val handleX = (seam - touch / 2).coerceIn(0.dp, maxX)
        val description = when {
            !split.showList -> "List collapsed. Drag toward the conversation to show the list."
            !split.showDetail -> "Conversation collapsed. Drag toward the list to show it."
            else -> "Drag to resize the list and conversation. Drag to either edge to collapse a pane."
        }
        val interaction = remember { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()
        var dragging by remember { mutableStateOf(false) }
        Box(
            Modifier
                .offset(x = handleX)
                .width(touch)
                .fillMaxHeight()
                .hoverable(interaction)
                .pointerInput(widthPx, layoutDirection) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { _, amount ->
                        val delta = if (layoutDirection == LayoutDirection.Rtl) -amount else amount
                        drag.value(delta, widthPx)
                    }
                }
                .semantics { contentDescription = description },
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(if (dragging || hovered) 6.dp else 4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (dragging || hovered) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
            )
        }
    }
}

private class SlidingPillState {
    val leadingEdge = Animatable(0f)
    val trailingEdge = Animatable(0f)
    var placed by mutableStateOf(false)

    val center: Float get() = (leadingEdge.value + trailingEdge.value) / 2f
    val leading: Float get() = leadingEdge.value
    val length: Float get() = (trailingEdge.value - leadingEdge.value).coerceAtLeast(0f)

    suspend fun moveTo(targetCenterPx: Float, targetHalfPx: Float, reducedMotion: Boolean) {
        if (targetCenterPx <= 0f || targetHalfPx <= 0f) return
        val start = targetCenterPx - targetHalfPx
        val end = targetCenterPx + targetHalfPx
        if (!placed || reducedMotion) {
            leadingEdge.snapTo(start)
            trailingEdge.snapTo(end)
            placed = true
            return
        }
        if (leadingEdge.value == start && trailingEdge.value == end) return
        val movingForward = targetCenterPx > center
        val lead = spring<Float>(dampingRatio = 0.70f, stiffness = 550f)
        val trail = spring<Float>(dampingRatio = 0.70f, stiffness = 300f)
        coroutineScope {
            launch { leadingEdge.animateTo(start, if (movingForward) trail else lead) }
            launch { trailingEdge.animateTo(end, if (movingForward) lead else trail) }
        }
    }
}

private fun pillCovers(pillCenterPx: Float, iconCenterPx: Float, halfPx: Float): Boolean {
    if (iconCenterPx <= 0f || halfPx <= 0f) return false
    return abs(pillCenterPx - iconCenterPx) < halfPx
}
