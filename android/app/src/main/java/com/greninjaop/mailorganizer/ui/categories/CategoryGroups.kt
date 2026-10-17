package com.greninjaop.mailorganizer.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.ui.mail.visuals
import com.greninjaop.mailorganizer.ui.theme.MoDarkCategoryColors
import com.greninjaop.mailorganizer.ui.theme.MoLightCategoryColors

/**
 * Presentation layer for categories (2026-10-12).
 *
 * The [MailCategory] enum and the classification engine are untouched —
 * this only groups categories for display so the Categories screen shows
 * five groups instead of eleven rows. Each group carries ONE semantic
 * accent (2026-10-17: the user asked for color that respects the icon's
 * job — the old "neutral icon" rule is superseded). The accent comes
 * from the centralized category tokens, so light/dark stay consistent:
 * warning → amber, shopping → orange, notifications → sky blue,
 * work → royal blue, general mail → brand violet. Color is never the
 * indicator alone — the icon + text label always carry the meaning.
 */
data class CategoryGroup(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val members: List<MailCategory>,
    /** Semantic accent, light theme. */
    val accentLight: Color,
    /** Semantic accent, dark theme. */
    val accentDark: Color,
) {
    /** "Action required · Important · Security" — names the contained categories. */
    val subtitle: String get() = members.joinToString(" · ") { it.visuals().label }
}

val CATEGORY_GROUPS: List<CategoryGroup> = listOf(
    CategoryGroup(
        id = "needs_attention",
        title = "Needs attention",
        icon = Icons.Filled.Warning,
        // Warning triangle → amber (the security token is the warning amber).
        accentLight = MoLightCategoryColors.security,
        accentDark = MoDarkCategoryColors.security,
        members = listOf(
            MailCategory.ACTION_REQUIRED,
            MailCategory.IMPORTANT,
            MailCategory.SECURITY,
        ),
    ),
    CategoryGroup(
        id = "promotions",
        title = "Promotions",
        icon = Icons.Filled.ShoppingCart,
        // Shopping cart → orange (deals/shopping).
        accentLight = MoLightCategoryColors.promotions,
        accentDark = MoDarkCategoryColors.promotions,
        members = listOf(
            MailCategory.PROMOTIONS,
            MailCategory.NEWSLETTERS,
        ),
    ),
    CategoryGroup(
        id = "updates",
        title = "Updates",
        icon = Icons.Filled.Notifications,
        // Bell → sky blue (notifications).
        accentLight = MoLightCategoryColors.notifications,
        accentDark = MoDarkCategoryColors.notifications,
        members = listOf(
            MailCategory.NOTIFICATIONS,
            MailCategory.RECEIPTS_ORDERS,
        ),
    ),
    CategoryGroup(
        id = "work_learning",
        title = "Work & learning",
        icon = Icons.Filled.Build,
        // Wrench → royal blue (career/work).
        accentLight = MoLightCategoryColors.career,
        accentDark = MoDarkCategoryColors.career,
        members = listOf(
            MailCategory.CAREER,
            MailCategory.EDUCATION,
        ),
    ),
    CategoryGroup(
        id = "everything_else",
        title = "Everything else",
        icon = Icons.Filled.Email,
        // Envelope → brand violet (general mail; the `important` token
        // is the brand violet in both themes).
        accentLight = MoLightCategoryColors.important,
        accentDark = MoDarkCategoryColors.important,
        members = listOf(
            MailCategory.LOW_VALUE,
            MailCategory.UNCLASSIFIED,
        ),
    ),
)

/** Total messages across the group's member categories. */
fun CategoryGroup.totalCount(counts: Map<MailCategory, Int>): Int =
    members.sumOf { counts[it] ?: 0 }
