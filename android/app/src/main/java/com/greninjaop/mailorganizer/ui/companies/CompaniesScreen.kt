package com.greninjaop.mailorganizer.ui.companies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.CategoryChip
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.mail.MailRowLayout
import com.greninjaop.mailorganizer.ui.mail.visuals
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import androidx.compose.foundation.layout.WindowInsets
import com.greninjaop.mailorganizer.ui.theme.MoCard
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/** Factory for the Phase 11 Companies destination. */
class CompaniesViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CompaniesViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            dispatchers = container.dispatchers,
            activeAccountPreferences = container.activeAccountPreferences,
        ) as T
    }
}

/**
 * Companies destination — Phase 11 (phase §25–§28).
 *
 * Company → senders → messages. Each row shows the company name, domain,
 * and a real message count (§26). The list never overloads: name, domain,
 * count, nothing more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompaniesScreen(
    viewModel: CompaniesViewModel,
    onNavigate: (String) -> Unit,
    onOpenCompany: (companyId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        // No top bar here — it is hoisted above the NavHost (see
        // MoPrimaryTopBar). No window insets either: the hoisted top
        // bar and bottom bar own the status/navigation insets, so this
        // Scaffold must not add them again.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (val content = uiState.content) {
            is CompaniesContent.Loading -> MoLoadingState(
                message = "Loading companies…",
                modifier = Modifier.padding(padding),
            )
            is CompaniesContent.Ready -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = MoSpacing.md,
                    vertical = MoSpacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                items(content.companies, key = { it.record.companyId }) { entry ->
                    val name = entry.record.userOverrideName
                        ?: entry.record.canonicalName
                    MoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MoSpacing.companyRowMinHeight)
                            .animateItem()
                            .semantics {
                                contentDescription =
                                    "$name, ${entry.messageCount} messages"
                            },
                        onClick = { onOpenCompany(entry.record.companyId) },
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompanyLogo(
                            domain = entry.record.normalizedDomain,
                            name = name,
                        )
                        Spacer(Modifier.width(MoSpacing.sm))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = entry.record.normalizedDomain,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${entry.messageCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    }
                }
            }
            is CompaniesContent.Empty -> MoEmptyState(
                title = "No companies yet",
                message = if (content.hasAccount) {
                    "Companies appear here once your mail is organized. " +
                        "Senders that can't be resolved to a company are never hidden " +
                        "— they simply have no company row."
                } else {
                    "Connect a Gmail account to start organizing your mail."
                },
                modifier = Modifier.padding(padding),
            )
            is CompaniesContent.Error -> MoErrorState(
                message = content.message,
                onRetry = { /* re-collect on next subscription */ },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/**
 * Company detail — Phase 11 (phase §27): company name, domains, recent
 * messages, category distribution. Deliberately not a full analytics
 * dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDetailScreen(
    viewModel: CompaniesViewModel,
    onOpenThread: (threadId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail by viewModel.detail.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Text(
                        when (val d = detail) {
                            is CompanyDetailContent.Loaded ->
                                d.company.userOverrideName ?: d.company.canonicalName
                            else -> "Company"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to companies",
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val content = detail) {
            is CompanyDetailContent.Loading -> MoLoadingState(
                message = "Loading company…",
                modifier = Modifier.padding(padding),
            )
            is CompanyDetailContent.Loaded -> {
                val name = content.company.userOverrideName
                    ?: content.company.canonicalName
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = MoSpacing.md,
                        vertical = MoSpacing.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                ) {
                    item {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.sm,
                            ),
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = content.company.normalizedDomain,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(MoSpacing.xs))
                            Text(
                                text = "${content.messages.size} messages",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (content.categoryCounts.isNotEmpty()) {
                                Spacer(Modifier.height(MoSpacing.xs))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                                ) {
                                    content.categoryCounts.entries
                                        .sortedByDescending { it.value }
                                        .take(4)
                                        .forEach { (category, count) ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                CategoryChip(category = category)
                                                Text(
                                                    text = "$count",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                }
                            }
                        }
                    }
                    items(content.messages, key = { it.messageId }) { message ->
                        // Gmail-exact row (shared with the main mail list).
                        // The per-message category chip is intentionally not
                        // shown here — Gmail rows carry no chips, and this is
                        // already a company-filtered view.
                        val timeText = MailFormatting.relativeTime(
                            message.timestampEpochMs,
                            System.currentTimeMillis(),
                        )
                        MailRowLayout(
                            sender = message.senderName,
                            senderKey = message.senderName,
                            // Every row in a company view shows that
                            // company's logo (2026-10-13), not a letter.
                            senderDomain = content.company.normalizedDomain,
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
            is CompanyDetailContent.NotFound -> MoEmptyState(
                title = "Company not found",
                message = "This company is no longer in the list.",
                actionLabel = "Back",
                onAction = onBack,
                modifier = Modifier.padding(padding),
            )
            is CompanyDetailContent.Error -> MoErrorState(
                message = content.message,
                onRetry = onBack,
                retryLabel = "Back",
                modifier = Modifier.padding(padding),
            )
        }
    }
}
