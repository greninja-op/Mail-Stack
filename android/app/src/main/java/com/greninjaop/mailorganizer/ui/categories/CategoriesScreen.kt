package com.greninjaop.mailorganizer.ui.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.mail.MailRowLayout
import com.greninjaop.mailorganizer.ui.mail.visuals
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.navigation.MoAppTopBar
import com.greninjaop.mailorganizer.ui.navigation.MoNavBar
import com.greninjaop.mailorganizer.ui.navigation.PrimaryDestination
import com.greninjaop.mailorganizer.ui.theme.GlassCard
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/** Factory for the Phase 11 Categories destination. */
class CategoriesViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CategoriesViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            dispatchers = container.dispatchers,
            activeAccountPreferences = container.activeAccountPreferences,
        ) as T
    }
}

/**
 * Categories destination — Phase 11 (phase §21–§24), regrouped 2026-10-12.
 *
 * Category browsing backed by Phase 7 classification. Counts are real
 * local counts (§23); empty categories get an honest empty state (§24).
 *
 * Presentation (2026-10-12): instead of eleven per-category colored rows,
 * five calm groups ("Needs attention", "Promotions", "Updates",
 * "Work & learning", "Everything else") — each with one NEUTRAL tonal icon
 * and text labels. The [MailCategory] enum and the classifier are untouched;
 * this is a display grouping only. Tapping a group expands it inline with
 * animation, revealing its member categories; tapping a member opens the
 * existing category detail destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigate: (String) -> Unit,
    onOpenCategory: (MailCategory) -> Unit,
    onOpenThread: (threadId: String) -> Unit,
    onOpenSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsState()
    val account by viewModel.currentAccount.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            MoAppTopBar(
                                account = account,
                onOpenSearch = onOpenSearch,
                onOpenAccounts = { onNavigate(AppDestinations.ACCOUNTS) },
                onOpenSecondary = onNavigate,
            )
        },
        bottomBar = {
            MoNavBar(
                currentRoute = PrimaryDestination.CATEGORIES.route,
                onNavigate = onNavigate,
            )
        },
    ) { padding ->
        when (val content = uiState.content) {
            is CategoriesContent.Loading -> MoLoadingState(
                message = "Loading categories…",
                modifier = Modifier.padding(padding),
            )
            is CategoriesContent.Ready -> CategoryGroupList(
                counts = content.counts,
                onOpenCategory = onOpenCategory,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
            is CategoriesContent.Empty -> MoEmptyState(
                title = "No categories yet",
                message = if (content.hasAccount) {
                    "Mail hasn't been classified yet. Categories appear here once " +
                        "your mail is organized."
                } else {
                    "Connect a Gmail account to start organizing your mail."
                },
                modifier = Modifier.padding(padding),
            )
            is CategoriesContent.Error -> MoErrorState(
                message = content.message,
                onRetry = { /* re-collect on next subscription */ },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/**
 * The five presentation groups as an M3 list. One row per group: neutral
 * tonal icon container, group name + total count, subtitle naming the
 * contained categories. Tapping expands inline (animated) to the member
 * categories, which navigate to the existing detail destination.
 */
@Composable
private fun CategoryGroupList(
    counts: Map<MailCategory, Int>,
    onOpenCategory: (MailCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val groups = remember(counts) {
        CATEGORY_GROUPS.mapNotNull { group ->
            val total = group.totalCount(counts)
            if (total > 0) group to total else null
        }
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = MoSpacing.md,
            vertical = MoSpacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
    ) {
        items(groups, key = { (group, _) -> group.id }) { (group, total) ->
            val expanded = expandedGroups[group.id] == true
            val members = remember(group, counts) {
                group.members.filter { (counts[it] ?: 0) > 0 }
            }
            GlassCard(
                modifier = Modifier
                    .animateItem()
                    .semantics {
                        contentDescription =
                            "${group.title}, $total messages. " +
                                if (expanded) "Expanded." else "Collapsed. Tap to expand."
                    },
                onClick = { expandedGroups[group.id] = !expanded },
            ) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(group.title) },
                supportingContent = {
                    Column {
                        Text(
                            text = "$total messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = group.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(MoSpacing.xxxl)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = group.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingContent = {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            }
            // Inline expansion, animated: member categories as sub-rows.
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    members.forEach { member ->
                        val label = remember(member) { member.visuals().label }
                        val count = counts[member] ?: 0
                        GlassCard(
                            modifier = Modifier
                                .padding(start = MoSpacing.xl)
                                .semantics {
                                    contentDescription = "$label, $count messages"
                                },
                            onClick = { onOpenCategory(member) },
                        ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(label) },
                            trailingContent = {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category detail — messages in one category (phase §22).
 *
 * Shown as a separate route (`category/{name}`); the category name stays
 * visible as the title so context is never lost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    viewModel: CategoriesViewModel,
    category: MailCategory,
    onOpenThread: (threadId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail by viewModel.detail.collectAsState()
    val visuals = category.visuals()

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = { Text(visuals.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to categories",
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val content = detail) {
            is CategoryDetailContent.Loading -> MoLoadingState(
                message = "Loading ${visuals.label.lowercase()}…",
                modifier = Modifier.padding(padding),
            )
            is CategoryDetailContent.Loaded -> {
                if (content.messages.isEmpty()) {
                    MoEmptyState(
                        title = "No ${visuals.label.lowercase()} emails yet",
                        message = "Nothing in this category right now.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = MoSpacing.md,
                            vertical = MoSpacing.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                    ) {
                        item {
                            Text(
                                text = "${content.messages.size} messages",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = MoSpacing.md,
                                    vertical = MoSpacing.xs,
                                ),
                            )
                        }
                        items(content.messages, key = { it.messageId }) { message ->
                            // Gmail-exact row (shared with the main mail list):
                            // 40dp avatar, sender+time / subject / snippet,
                            // 88dp minimum — not the old compact row.
                            val timeText = MailFormatting.relativeTime(
                                message.timestampEpochMs,
                                System.currentTimeMillis(),
                            )
                            MailRowLayout(
                                sender = message.senderName,
                                senderKey = message.senderName,
                                subject = MailFormatting.subjectDisplay(message.subject),
                                snippet = "",
                                timeText = timeText,
                                unread = message.unread,
                                showStar = false,
                                showAttachment = false,
                                messageCount = 1,
                                priority = null,
                                account = null,
                                showAccountIndicator = false,
                                onClick = { onOpenThread(message.threadId) },
                                modifier = Modifier
                                    .animateItem()
                                    .semantics {
                                        contentDescription =
                                            "${if (message.unread) "Unread email" else "Email"} " +
                                                "from ${message.senderName}, " +
                                                "subject: ${MailFormatting.subjectDisplay(message.subject)}" +
                                                (if (timeText.isNotBlank()) ", $timeText" else "")
                                    },
                            )
                        }
                    }
                }
            }
            is CategoryDetailContent.Error -> MoErrorState(
                message = content.message,
                onRetry = onBack,
                retryLabel = "Back",
                modifier = Modifier.padding(padding),
            )
        }
    }
}
