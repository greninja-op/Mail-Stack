package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.ui.components.MailListSkeleton
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.edgeFade
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Core inbox screen (Phase 6).
 *
 * Gmail-familiar chrome (design.md): the top bar — drawer affordance,
 * search field and account avatar — is hoisted above the NavHost (see
 * MoPrimaryTopBar in ui/navigation), so the search pill can reshape
 * itself across tab switches; this screen owns the navigation drawer
 * with the six mailbox destinations (All Inbox / Primary / Promotional
 * / Social / Spam / Starred). Classification-backed destinations show
 * honest empty states until Phase 7 — never fabricated categories (§59).
 *
 * Read-only: no mark-read/star/archive actions (phase §6, Phase 22 owns
 * Gmail writes). The refresh action delegates to Phase 4's sync engine and
 * reports its honest outcome.
 *
 * The list has exactly three states: content, empty, skeleton (loading).
 * While the first batch loads, a Boneyard-style skeleton mirrors the
 * real rows' geometry so the layout never jumps when content arrives.
 */
@Composable
fun MailScreen(
    viewModel: MailViewModel,
    onOpenThread: (threadId: String, focusMessageId: String?) -> Unit,
    /**
     * Drawer-open requests from the hamburger in the hoisted top bar:
     * the counter increments per tap; the drawer opens on each change.
     */
    drawerOpenSignal: StateFlow<Int>,
    /**
     * True when Mail was entered via a primary-tab switch (vs popping
     * back from a pushed route). The heavy lists defer past the
     * tab-switch transition so the bubble/slide/pill animations run
     * without competing against the list composition.
     */
    deferList: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.syncUi) {
        val result = state.syncUi as? SyncUiState.Result ?: return@LaunchedEffect
        snackbar.showSnackbar(result.message)
        viewModel.consumeSyncResult()
    }

    // Drawer requests from the hoisted top bar's hamburger.
    val drawerSignal by drawerOpenSignal.collectAsState()
    LaunchedEffect(drawerSignal) {
        if (drawerSignal > 0) drawerState.open()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MailDrawer(
                state = state,
                onSelect = { dest ->
                    viewModel.setDestination(dest)
                    scope.launch { drawerState.close() }
                },
            )
        },
        modifier = modifier,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            // No top bar here — it is hoisted above the NavHost (see
            // MoPrimaryTopBar). No window insets either: the hoisted
            // top bar and bottom bar own the status/navigation insets,
            // so this Scaffold must not add them again.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (state.isOffline) {
                    OfflineBanner()
                }
                val sync = state.syncUi
                if (sync is SyncUiState.Syncing) {
                    // Determinate when the total is known (initial sync),
                    // indeterminate otherwise (incremental) — 2026-10-12.
                    if (sync.progress != null) {
                        LinearProgressIndicator(
                            progress = { sync.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        text = syncProgressLabel(sync, sync.stageText),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = MoSpacing.md,
                            vertical = MoSpacing.xs,
                        ),
                    )
                }
                MailContent(
                    state = state,
                    onOpenThread = onOpenThread,
                    onRetry = viewModel::refresh,
                    onLoadMore = viewModel::loadMore,
                    isLoadingMore = isLoadingMore,
                    onClearFilter = viewModel::clearFilter,
                    deferList = deferList,
                    modifier = Modifier.weight(1f),
                    header = {
                        // Phase 9: action-required view toggle — global
                        // "what needs my attention" filter.
                        ActionRequiredFilterChip(
                            active = state.actionRequiredOnly,
                            onToggle = viewModel::toggleActionRequired,
                            modifier = Modifier.padding(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.xs,
                            ),
                        )
                        // Phase 8: company filter lives inside the selected
                        // category — never a navigation destination.
                        if (state.companyFilter.visible) {
                            CompanyFilterRow(
                                uiState = state.companyFilter,
                                onSelect = viewModel::selectCompany,
                                onTogglePin = viewModel::toggleCompanyPin,
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * Sync progress label (2026-10-12): "42% · 210 of ~500 emails · Fetching mail…".
 * Falls back to the stage text alone when the total is unknown.
 */
private fun syncProgressLabel(sync: SyncUiState.Syncing, stageText: String): String {
    val total = sync.totalEstimate
    val pct = sync.progress
    if (total == null || total <= 0 || pct == null) return stageText
    // Gmail's estimate is rough — once processed passes it, drop the
    // comparison instead of showing "253 of ~201".
    val count = if (sync.processed > total) "${sync.processed} emails"
    else "${sync.processed} of ~$total emails"
    return "${(pct * 100).toInt()}% · $count · $stageText"
}

@Composable
private fun MailDrawer(
    state: MailScreenState,
    onSelect: (MailboxDestination) -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val account = state.activeAccount
        Column(
            modifier = Modifier.padding(MoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
        ) {
            if (state.isUnified) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MailOutline,
                        contentDescription = null,
                        modifier = Modifier.size(MoSpacing.huge),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(MoSpacing.sm))
                    Column {
                        Text(
                            text = "All Accounts",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Unified Inbox",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = MoSpacing.xs))
            } else if (account != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountAvatar(account = account, size = MoSpacing.huge)
                    Spacer(Modifier.width(MoSpacing.sm))
                    Column {
                        Text(
                            text = account.displayName ?: account.emailAddress,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = account.emailAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = MoSpacing.xs))
            }
            MailboxDestination.entries.forEach { dest ->
                NavigationDrawerItem(
                    label = { Text(dest.title) },
                    selected = state.destination == dest,
                    onClick = { onSelect(dest) },
                    icon = {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

private val MailboxDestination.icon
    @Composable get() = when (this) {
        MailboxDestination.ALL_INBOX -> Icons.Filled.Email
        MailboxDestination.PRIMARY -> Icons.Filled.MailOutline
        MailboxDestination.PROMOTIONAL -> Icons.Filled.ShoppingCart
        MailboxDestination.SOCIAL -> Icons.Filled.Person
        MailboxDestination.SPAM -> Icons.Filled.Warning
        MailboxDestination.STARRED -> Icons.Filled.Star
    }


@Composable
private fun OfflineBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(MoSpacing.xs))
            Text(
                text = "You're offline — showing synced mail",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MailContent(
    state: MailScreenState,
    onOpenThread: (threadId: String, focusMessageId: String?) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean = false,
    onClearFilter: () -> Unit,
    deferList: Boolean = false,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
        // Gmail parity: pull-to-refresh on the list, wired to the sync engine.
        val isRefreshing = state.syncUi is SyncUiState.Syncing
        // Defer the heavy lists past the tab-switch transition (2026-10-16):
        // the skeleton holds the list area for the first 300ms after
        // entering via a tab switch, so the bubble/slide/pill animations
        // run without competing against the list composition. Popping
        // back from a pushed route shows the list immediately.
        var listReady by remember { mutableStateOf(!deferList) }
        LaunchedEffect(deferList) {
            if (deferList) {
                delay(300)
                listReady = true
            }
        }
        Column(modifier = modifier.fillMaxSize()) {
            header()
            when (val content = state.content) {
                // Skeleton loading (2026-10-16): while the first batch
                // loads, show skeleton rows mirroring the real rows'
                // geometry — never a spinner, never a layout jump.
                MailboxContent.Loading -> MailListSkeleton(
                    modifier = Modifier.weight(1f),
                )
                is MailboxContent.Threads -> {
                    if (!listReady) {
                        // Deferring the list past the tab-switch
                        // transition — the skeleton holds this area.
                        MailListSkeleton(modifier = Modifier.weight(1f))
                    } else {
                    val listState = rememberLazyListState()
                    // Gmail-style infinite scroll (2026-10-16): prefetch
                    // the next page when the user scrolls past the halfway
                    // mark of the loaded list — by the time they reach the
                    // end, the next batch is already there. No "Load more"
                    // button, no waiting at the end. The ViewModel guards
                    // loadMore() against stacked requests.
                    LaunchedEffect(listState, content) {
                        snapshotFlow {
                            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                        }.collect { lastVisible ->
                            if (shouldPrefetchMore(
                                    lastVisible,
                                    content.items.size,
                                    content.hasMore,
                                )
                            ) {
                                onLoadMore()
                            }
                        }
                    }
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRetry,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                                // Subtle top/bottom edge fade (2026-10-16):
                                // rows dissolve gently as they scroll under
                                // the bars instead of hard-clipping.
                                .edgeFade(
                                    top = listState.canScrollBackward,
                                    bottom = listState.canScrollForward,
                                ),
                            contentPadding = PaddingValues(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.sm,
                            ),
                            verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                        ) {
                            items(content.items, key = { it.threadId }) { item ->
                                val rowAccount = state.accountsById[item.accountId] ?: state.activeAccount
                                ThreadRow(
                                    item = item,
                                    account = rowAccount,
                                    showAccountIndicator = state.isUnified || state.destination == MailboxDestination.ALL_INBOX,
                                    onClick = { onOpenThread(item.threadId, null) },
                                    modifier = Modifier.animateItem(),
                                )
                                // No dividers between rows — Gmail has none.
                            }
                            if (isLoadingMore) {
                                item { LoadingMoreFooter() }
                            }
                        }
                    }
                    }
                }
                is MailboxContent.Messages -> {
                    if (!listReady) {
                        // Deferring the list past the tab-switch
                        // transition — the skeleton holds this area.
                        MailListSkeleton(modifier = Modifier.weight(1f))
                    } else {
                    val listState = rememberLazyListState()
                    // Same infinite scroll as the thread list (2026-10-16):
                    // starred/promotions/social/spam lists used to stop
                    // silently at the first page with no way to see more.
                    LaunchedEffect(listState, content) {
                        snapshotFlow {
                            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                        }.collect { lastVisible ->
                            if (shouldPrefetchMore(
                                    lastVisible,
                                    content.items.size,
                                    content.hasMore,
                                )
                            ) {
                                onLoadMore()
                            }
                        }
                    }
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRetry,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                                // Subtle top/bottom edge fade (2026-10-16):
                                // rows dissolve gently as they scroll under
                                // the bars instead of hard-clipping.
                                .edgeFade(
                                    top = listState.canScrollBackward,
                                    bottom = listState.canScrollForward,
                                ),
                            contentPadding = PaddingValues(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.sm,
                            ),
                            verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                        ) {
                            items(content.items, key = { it.messageId }) { item ->
                                val rowAccount = state.accountsById[item.accountId] ?: state.activeAccount
                                StarredMessageRow(
                                    item = item,
                                    account = rowAccount,
                                    showAccountIndicator = state.isUnified,
                                    onClick = { onOpenThread(item.threadId, item.messageId) },
                                    modifier = Modifier.animateItem(),
                                )
                                // No dividers between rows — Gmail has none.
                            }
                            if (isLoadingMore) {
                                item { LoadingMoreFooter() }
                            }
                        }
                    }
                    }
                }
                is MailboxContent.Empty -> {
                    when (content.kind) {
                        EmptyKind.NO_MAIL -> MoEmptyState(
                            title = "No synchronized email yet",
                            message = "Connect a Gmail account to see your mail here. " +
                                "Tap Sync now to fetch your messages.",
                            actionLabel = "Sync now",
                            onAction = onRetry,
                            modifier = Modifier.weight(1f),
                        )
                        EmptyKind.NO_FILTER_RESULTS -> MoEmptyState(
                            title = "No matches",
                            message = "Nothing in this mailbox matches your search.",
                            actionLabel = "Clear search",
                            onAction = onClearFilter,
                            modifier = Modifier.weight(1f),
                        )
                        EmptyKind.NO_STARRED -> MoEmptyState(
                            title = "No starred mail",
                            message = "Messages you star will appear here.",
                            modifier = Modifier.weight(1f),
                        )
                        EmptyKind.NO_PROMOTIONS -> MoEmptyState(
                            title = "No promotions",
                            message = "Promotional mail will appear here once classified.",
                            modifier = Modifier.weight(1f),
                        )
                        EmptyKind.NO_SOCIAL -> MoEmptyState(
                            title = "No social updates",
                            message = "Social-network mail will appear here.",
                            modifier = Modifier.weight(1f),
                        )
                        EmptyKind.NO_SPAM -> MoEmptyState(
                            title = "No spam",
                            message = "Nothing filed as spam. Legitimate mail misfiled " +
                                "as spam can be recovered from Gmail for now.",
                            modifier = Modifier.weight(1f),
                        )
                        EmptyKind.NO_COMPANY_RESULTS -> MoEmptyState(
                            title = "No mail from this company here",
                            message = "Nothing from the selected company in this mailbox.",
                            modifier = Modifier.weight(1f),
                        )
                        EmptyKind.NO_ACTION_REQUIRED -> MoEmptyState(
                            title = "Nothing needs action",
                            message = "No mail classified as action-required. " +
                                "You're all caught up.",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                is MailboxContent.Error -> MoErrorState(
                    message = content.message,
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f),
                )
            }
        }
}

/**
 * Infinite-scroll prefetch trigger (2026-10-16, Gmail parity).
 *
 * Returns true once the user has scrolled past the halfway mark of the
 * currently loaded list while more mail exists — the next page starts
 * loading then, so it is already there by the time the user reaches
 * the end. Pure function, unit-tested.
 */
internal fun shouldPrefetchMore(
    lastVisibleIndex: Int,
    itemCount: Int,
    hasMore: Boolean,
): Boolean {
    if (!hasMore || itemCount <= 0 || lastVisibleIndex < 0) return false
    return lastVisibleIndex >= itemCount / 2
}

/** Small centered spinner shown at the list end while the next page loads. */
@Composable
private fun LoadingMoreFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MoSpacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(MoSpacing.lg),
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(MoSpacing.sm))
        Text(
            text = "Loading more…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
