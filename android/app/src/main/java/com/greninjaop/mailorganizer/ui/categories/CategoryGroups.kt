package com.greninjaop.mailorganizer.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.ui.mail.visuals

/**
 * Presentation layer for categories (2026-10-12).
 *
 * The [MailCategory] enum and the classification engine are untouched —
 * this only groups categories for display so the Categories screen shows
 * five calm groups instead of eleven rainbow-colored rows. Each group has
 * one NEUTRAL icon (never a per-category color); meaning always comes
 * from the text labels.
 */
data class CategoryGroup(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val members: List<MailCategory>,
) {
    /** "Action required · Important · Security" — names the contained categories. */
    val subtitle: String get() = members.joinToString(" · ") { it.visuals().label }
}

val CATEGORY_GROUPS: List<CategoryGroup> = listOf(
    CategoryGroup(
        id = "needs_attention",
        title = "Needs attention",
        icon = Icons.Filled.Warning,
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
        members = listOf(
            MailCategory.PROMOTIONS,
            MailCategory.NEWSLETTERS,
        ),
    ),
    CategoryGroup(
        id = "updates",
        title = "Updates",
        icon = Icons.Filled.Notifications,
        members = listOf(
            MailCategory.NOTIFICATIONS,
            MailCategory.RECEIPTS_ORDERS,
        ),
    ),
    CategoryGroup(
        id = "work_learning",
        title = "Work & learning",
        icon = Icons.Filled.Build,
        members = listOf(
            MailCategory.CAREER,
            MailCategory.EDUCATION,
        ),
    ),
    CategoryGroup(
        id = "everything_else",
        title = "Everything else",
        icon = Icons.Filled.Email,
        members = listOf(
            MailCategory.LOW_VALUE,
            MailCategory.UNCLASSIFIED,
        ),
    ),
)

/** Total messages across the group's member categories. */
fun CategoryGroup.totalCount(counts: Map<MailCategory, Int>): Int =
    members.sumOf { counts[it] ?: 0 }
