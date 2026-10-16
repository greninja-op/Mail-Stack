package com.greninjaop.mailorganizer.ui.navigation

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Bottom navigation bar for the five primary destinations (Phase 11,
 * phase §7): Home, Mail, Categories, Companies, Actions.
 *
 * 2026-10-16 (Expressive redesign): solid surfaceContainer bar, no
 * border, no translucency. The selected item gets a secondaryContainer
 * pill indicator (Material 3 Expressive) with an onSecondaryContainer
 * icon; the label stays onSurface. System navigation insets are handled
 * by the hosting Scaffold.
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
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = modifier,
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
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
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
