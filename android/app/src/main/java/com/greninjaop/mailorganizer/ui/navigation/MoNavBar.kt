package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Bottom navigation bar for the five primary destinations (Phase 11,
 * phase §7): Home, Mail, Categories, Companies, Actions.
 *
 * 2026-10-16 (Expressive redesign): solid surfaceContainer bar, no
 * border, no translucency. The selected item gets a secondaryContainer
 * pill indicator (Material 3 Expressive) with an onSecondaryContainer
 * icon; the label stays onSurface. System navigation insets are handled
 * by the bar itself ([navigationBarsPadding]); the hosting Scaffold
 * applies no extra window insets.
 *
 * 2026-10-16 (jelly bubble): the indicator is a SINGLE pill that slides
 * between items instead of one pill per item that pops in/out. The
 * motion follows the squash-and-stretch principle (Disney's 12
 * principles; the same physics as a toggle-switch ball): the pill
 * stretches along its direction of travel while the trailing edge lags
 * behind, then the trailing edge catches up and the pill settles with
 * a small spring wobble. Implementation: the pill's left and right
 * edges are two independent [Animatable]s — the leading edge runs a
 * stiffer spring (fast start) and the trailing edge a softer one
 * (lags, so the pill elongates mid-flight), both underdamped so the
 * pill overshoots slightly and shakes into place. Vertical squash is
 * derived from the stretch (volume conservation: wider → flatter).
 * The icon tint follows the pill — an icon tints the moment the pill
 * covers it — so the bubble and the icon read as one unit.
 *
 * The bar MUST be hoisted above the NavHost (see AppNavGraph): a
 * per-screen bar remounts on every tab switch, and a remounted pill
 * can only snap, never slide.
 *
 * Icon note: `material-icons-core` (the only icon artifact declared) has
 * no `Business` glyph, so Companies uses `AccountBox` (organization card
 * metaphor) — the text label and content description carry the meaning,
 * never the icon alone.
 */
@Composable
fun MoNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destinations = PrimaryDestination.entries
    val selectedIndex = destinations.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val density = LocalDensity.current
    val pillHalfWidthPx = with(density) { PillHalfWidth.toPx() }
    val pillHeightPx = with(density) { PillHeight.toPx() }
    val pillTopPx = with(density) { PillTopOffset.toPx() }

    // Center x of each item, in the bar's coordinate space. The Row
    // gives every item an equal weight, so the centers are pure math
    // from the bar's measured width — no per-item measurement needed.
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val centers = remember(destinations.size, barWidthPx) {
        List(destinations.size) { (it + 0.5f) * barWidthPx / destinations.size }
    }
    // The pill's two edges, animated independently for the stretch.
    val leftEdge = remember { Animatable(0f) }
    val rightEdge = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }

    val targetCenter = centers.getOrNull(selectedIndex) ?: 0f
    val (targetLeft, targetRight) = pillTargetEdges(targetCenter, pillHalfWidthPx)

    LaunchedEffect(selectedIndex, centers, pillHalfWidthPx) {
        if (targetCenter <= 0f) return@LaunchedEffect
        if (!initialized) {
            // First layout: appear at the selection, don't animate in.
            leftEdge.snapTo(targetLeft)
            rightEdge.snapTo(targetRight)
            initialized = true
            return@LaunchedEffect
        }
        val pillCenter = (leftEdge.value + rightEdge.value) / 2f
        val movingRight = targetCenter > pillCenter
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

    val pillWidth = rightEdge.value - leftEdge.value
    val pillCenter = (leftEdge.value + rightEdge.value) / 2f
    val squash = pillSquashScaleY(pillWidth, pillHalfWidthPx * 2f)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .navigationBarsPadding(),
        ) {
            // The bubble, behind the icons.
            Box(
                Modifier
                    .offset { IntOffset(leftEdge.value.roundToInt(), pillTopPx.roundToInt()) }
                    .size(
                        width = with(density) { pillWidth.toDp() },
                        height = PillHeight,
                    )
                    .graphicsLayer { scaleY = squash }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .onPlaced { coords -> barWidthPx = coords.size.width.toFloat() },
            ) {
                destinations.forEachIndexed { index, dest ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(role = Role.Tab) {
                                if (index != selectedIndex) onNavigate(dest.route)
                            }
                            .semantics {
                                contentDescription = "${dest.label} tab"
                                selected = index == selectedIndex
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Spacer(Modifier.height(PillTopOffset))
                        Box(
                            Modifier.size(PillHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize),
                                tint = if (pillCoversIcon(pillCenter, centers[index], pillHalfWidthPx)) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Bar height (Material 3 NavigationBar), excluding the nav-bar inset. */
private val BarHeight = 80.dp

/** The bubble: Material 3 active-indicator size. */
private val PillHalfWidth = 32.dp
private val PillHeight = 32.dp

/** The bubble sits 12dp from the bar's top, centred on the icon. */
private val PillTopOffset = 12.dp

/** Material 3 NavigationBar icon size. */
private val IconSize = 24.dp

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
 * True when the bubble covers an icon — the icon tints the moment the
 * bubble arrives over it, so bubble + icon read as one unit.
 * Pure function, unit-tested.
 */
internal fun pillCoversIcon(pillCenterPx: Float, iconCenterPx: Float, halfWidthPx: Float): Boolean =
    abs(pillCenterPx - iconCenterPx) < halfWidthPx

/** Primary destinations in display order (phase §7). */
enum class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(AppDestinations.HOME, "Home", Icons.Filled.Home),
    MAIL(AppDestinations.MAIL, "Mail", Icons.Filled.Email),
    CATEGORIES(AppDestinations.CATEGORIES, "Categories", Icons.Filled.List),
    COMPANIES(AppDestinations.COMPANIES, "Companies", Icons.Filled.AccountBox),
    ACTIONS(AppDestinations.ACTIONS, "Actions", Icons.Filled.CheckCircle),
}
