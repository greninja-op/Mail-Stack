package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.ui.theme.rememberGlassPalette

/**
 * Bottom navigation bar for the five primary destinations (Phase 11,
 * phase §7): Home, Mail, Categories, Companies, Actions.
 *
 * 2026-10-13 (Breeze redesign): frosted-glass bar, FLUSH with the bottom
 * of the screen (never floating). Translucent bar color + hairline top
 * border; the selected item gets a solid primary pill with a white icon
 * and primary label, matching the reference. System navigation insets are
 * handled by the hosting Scaffold.
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
    val glass = rememberGlassPalette()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = glass.barBorder,
            ),
    ) {
        NavigationBar(
            containerColor = glass.bar,
            tonalElevation = 0.dp,
        ) {
            PrimaryDestination.entries.forEach { dest ->
                val selected = currentRoute == dest.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) onNavigate(dest.route) },
                    icon = {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(dest.label) },
                    colors = NavigationBarItemDefaults.colors().copy(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        selectedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "${dest.label} tab"
                    },
                )
            }
        }
    }
}
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
