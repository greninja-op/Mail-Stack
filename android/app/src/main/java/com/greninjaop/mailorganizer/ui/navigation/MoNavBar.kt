package com.greninjaop.mailorganizer.ui.navigation

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.ui.components.JellyBubble
import com.greninjaop.mailorganizer.ui.components.pillCoversIcon
import com.greninjaop.mailorganizer.ui.components.rememberJellyBubbleState
import com.greninjaop.mailorganizer.ui.components.segmentCenters

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
 * pill is [JellyBubble] (see ui/components): squash-and-stretch physics
 * — the leading edge races ahead on a stiff spring while the trailing
 * edge lags, stretching the bubble mid-flight, then both settle with
 * a small wobble. The icon tint follows the pill — an icon tints the
 * moment the pill covers it — so the bubble and the icon read as one unit.
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

    // Center x of each item, in the bar's coordinate space. The Row
    // gives every item an equal weight, so the centers are pure math
    // from the bar's measured width — no per-item measurement needed.
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val centers = remember(destinations.size, barWidthPx) {
        segmentCenters(barWidthPx, destinations.size)
    }
    val bubble = rememberJellyBubbleState()

    val targetCenter = centers.getOrNull(selectedIndex) ?: 0f

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        // Modifier order matters here: navigationBarsPadding() is
        // OUTSIDE the fixed 80dp height, exactly like Material 3
        // NavigationBar's windowInsets. Total bar height is
        // 80dp + system-nav inset, with the content sitting clear
        // above the system navigation buttons. (2026-10-16 bug: the
        // padding was INSIDE the fixed height, so the inset squeezed
        // the content and the bar slid down onto the system buttons
        // — icons looked bigger and taps collided with the system nav.)
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(BarHeight),
        ) {
            // The bubble, behind the icons.
            JellyBubble(
                state = bubble,
                targetCenterPx = targetCenter,
                targetHalfWidthPx = pillHalfWidthPx,
                height = PillHeight,
                topOffset = PillTopOffset,
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
                                tint = if (pillCoversIcon(bubble.centerPx, centers[index], pillHalfWidthPx)) {
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
