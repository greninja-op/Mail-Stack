package com.greninjaop.mailstack.tablet.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Category groups for the local demo mailbox.
 *
 * Each group is one 28dp card: the header and its member rows share that
 * single rounded shape. On a wide pane the cards sit in a row.
 */
@Composable
fun TabletCategoriesPane() {
    val colors = workspaceColors()
    val groups = categoryGroups()
    var expandedRaw by rememberSaveable {
        mutableStateOf(groups.joinToString("|") { it.id })
    }
    val expanded = expandedRaw.split('|').filter { it.isNotEmpty() }.toSet()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (groups.isEmpty()) {
            WorkspaceEmpty(
                title = "No messages in these groups.",
                message = "Groups appear when the local demo mailbox has matching mail.",
                colors = colors,
            )
        } else {
            val columns = workspaceColumns(
                availableWidth = maxWidth - WorkspacePanePadding * 2,
                itemCount = groups.size,
                minItem = 300.dp,
                gap = 12.dp,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WorkspacePanePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                groups.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        row.forEach { group ->
                            CategoryCard(
                                group = group,
                                expanded = group.id in expanded,
                                colors = colors,
                                onToggle = {
                                    expandedRaw = toggleToken(expanded, group.id)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.Top),
                            )
                        }
                        repeat(columns - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    group: DemoGroup,
    expanded: Boolean,
    colors: WorkspaceColors,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (colors.dark) group.accentDark else group.accentLight
    val countLabel = workspaceMessageCount(group.messages.size)
    Surface(
        modifier = modifier.clip(WorkspaceCardShape),
        shape = WorkspaceCardShape,
        color = colors.card,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) "Collapses the group" else "Expands the group",
                        onClick = onToggle,
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${group.title}, $countLabel"
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = group.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                ) {
                    group.messages.forEach { message ->
                        WorkspaceMessageRow(message = message, colors = colors)
                    }
                }
            }
        }
    }
}

private data class DemoGroup(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val accentLight: Color,
    val accentDark: Color,
    val messages: List<WorkspaceDemo.Message>,
)

/**
 * Each check is independent, so one demo message can sit in more than one
 * group. Empty groups are left out. Names come only from demo messages.
 */
private fun categoryGroups(): List<DemoGroup> {
    val messages = WorkspaceDemo.messages
    val specs = listOf(
        GroupSpec(
            id = "needs-attention",
            title = "Needs attention",
            icon = Icons.Filled.Warning,
            accentLight = Color(0xFFB45309),
            accentDark = Color(0xFFFBBF24),
            include = { message ->
                message.unread && (
                    workspaceSubjectHas(message.subject, "sign-in") ||
                        workspaceSubjectHas(message.subject, "Security") ||
                        workspaceSubjectHas(message.subject, "alert")
                    )
            },
        ),
        GroupSpec(
            id = "promotions",
            title = "Promotions",
            icon = Icons.Filled.ShoppingCart,
            accentLight = Color(0xFFEA580C),
            accentDark = Color(0xFFFB923C),
            include = { it.bucket == WorkspaceDemo.Bucket.Promotional },
        ),
        GroupSpec(
            id = "updates",
            title = "Updates",
            icon = Icons.Filled.Notifications,
            accentLight = Color(0xFF0284C7),
            accentDark = Color(0xFF38BDF8),
            include = { message ->
                workspaceSubjectHas(message.subject, "bill") ||
                    workspaceSubjectHas(message.subject, "receipt") ||
                    workspaceSubjectHas(message.subject, "renew")
            },
        ),
        GroupSpec(
            id = "work",
            title = "Work & learning",
            icon = Icons.Filled.Build,
            accentLight = Color(0xFF1D4ED8),
            accentDark = Color(0xFF60A5FA),
            include = { message ->
                workspaceSubjectHas(message.subject, "pull request") ||
                    workspaceSubjectHas(message.subject, "Task") ||
                    workspaceSubjectHas(message.subject, "roadmap")
            },
        ),
        GroupSpec(
            id = "everything-else",
            title = "Everything else",
            icon = Icons.Filled.Email,
            accentLight = Color(0xFF5238E8),
            accentDark = Color(0xFFB9A8FF),
            include = { it.bucket == WorkspaceDemo.Bucket.Social },
        ),
    )
    return specs.mapNotNull { spec ->
        val members = messages.filter(spec.include)
        if (members.isEmpty()) {
            null
        } else {
            DemoGroup(
                id = spec.id,
                title = spec.title,
                icon = spec.icon,
                accentLight = spec.accentLight,
                accentDark = spec.accentDark,
                messages = members,
            )
        }
    }
}

private data class GroupSpec(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val accentLight: Color,
    val accentDark: Color,
    val include: (WorkspaceDemo.Message) -> Boolean,
)

private fun toggleToken(current: Set<String>, id: String): String {
    val next = current.toMutableSet()
    if (!next.add(id)) next.remove(id)
    return next.sorted().joinToString("|")
}
