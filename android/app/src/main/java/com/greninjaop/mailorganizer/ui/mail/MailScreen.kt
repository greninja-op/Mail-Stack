package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.greninjaop.mailorganizer.ui.components.MailListSkeleton
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.edgeFade
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Drag rate for the pull-to-refresh (2026-10-17): the list and the
 * indicator travel this fraction of the finger's distance. 0.5 means
 * a 192 dp finger pull moves the list 96 dp — the pull feels like
 * deliberate effort (Instagram-like), not a free scroll.
 */
internal const val PULL_DRAG_RATE = 0.5f

/**
 * Pull-to-refresh release threshold (2026-10-17): the list travel,
 * in dp, that must be reached before releasing arms a refresh.
 * Finger travel to trigger = threshold / [PULL_DRAG_RATE] = 192 dp.
 */
internal const val PULL_THRESHOLD_DP = 96

/**
 * Applies one pull drag delta to the current pull offset (px).
 * Positive [fingerDeltaPx] = pulling down (grows the pull, damped);
 * negative = scrolling back up (shrinks the pull, damped). The offset
 * never goes below 0. Pure, unit-tested.
 */
internal fun applyPullDrag(
    currentOffsetPx: Float,
    fingerDeltaPx: Float,
    dragRate: Float = PULL_DRAG_RATE,
): Float = (currentOffsetPx + fingerDeltaPx * dragRate).coerceAtLeast(0f)

/**
 * True when the pull offset has reached the release threshold — the
 * release arms a refresh. Pure, unit-tested.
 */
internal fun isPullPastThreshold(offsetPx: Float, thresholdPx: Float): Boolean =
    offsetPx >= thresholdPx

/**
 * Instagram-style pull-to-refresh container (2026-10-17), custom-built
 * to replace Material3's PullToRefreshBox.
 *
 * Why custom: M3's box fixes the threshold at 56 dp and drags 1:1, so
 * a normal scroll-to-top swipe (a fast ~100 dp finger flick) sailed
 * past the threshold and refreshed on its own. This version takes
 * real effort: [PULL_DRAG_RATE] damping + a [PULL_THRESHOLD_DP] list
 * travel threshold (~192 dp of finger) — a scroll-to-top swipe never
 * gets there.
 *
 * Behaviour:
 * - Pull-down overscroll (the list is at the top) grows the pull,
 *   damped; the content translates down by the pull and the pill
 *   fades in with the pull.
 * - Scrolling back up while pulled shrinks the pull first (the list
 *   only scrolls once the pull is back to 0).
 * - Release past the threshold: if [onRefresh] starts a sync, the
 *   pull settles at the threshold and holds while the sync runs,
 *   then springs back when it finishes. If [onRefresh] was a fast
 *   no-op (offline / demo / cooldown — it returns false), the pull
 *   springs back at once instead of hanging at the threshold.
 * - Release before the threshold: the pull springs back, no reload.
 * - While a sync runs, the pull is pinned (the list drag-scrolls
 *   normally underneath the held indicator).
 *
 * @param onRefresh starts a refresh; returns true when a sync was
 * actually started, false for a fast no-op.
 */
@Composable
private fun InstagramPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { PULL_THRESHOLD_DP.dp.toPx() }
    val pullOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val refreshingState = rememberUpdatedState(isRefreshing)
    // When the sync finishes, settle the pull back to rest.
    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (wasRefreshing && !isRefreshing) {
            pullOffset.animateTo(0f, tween(durationMillis = 250, easing = FastOutSlowInEasing))
        }
        wasRefreshing = isRefreshing
    }
    val connection = remember(thresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Scrolling back up while pulled: shrink the pull first.
                if (available.y >= 0f || refreshingState.value) return Offset.Zero
                val pull = pullOffset.value
                if (pull <= 0f) return Offset.Zero
                val consume = minOf(-available.y, pull / PULL_DRAG_RATE)
                scope.launch { pullOffset.snapTo((pull - consume * PULL_DRAG_RATE).coerceAtLeast(0f)) }
                return Offset(0f, -consume)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Leftover downward scroll (the list is at the top):
                // grow the pull, damped. Consume it so the list stays put.
                if (available.y <= 0f || refreshingState.value) return Offset.Zero
                scope.launch { pullOffset.snapTo(applyPullDrag(pullOffset.value, available.y)) }
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val pull = pullOffset.value
                if (pull <= 0f) return Velocity.Zero
                if (isPullPastThreshold(pull, thresholdPx) && !refreshingState.value) {
                    // Released past the threshold: start the refresh
                    // (the sync starts at once, no settle delay), then
                    // settle. A fast no-op (onRefresh false) springs
                    // back at once instead of hanging at the threshold.
                    val started = onRefresh()
                    if (started) {
                        pullOffset.animateTo(thresholdPx, tween(durationMillis = 150, easing = FastOutSlowInEasing))
                    } else {
                        pullOffset.animateTo(0f, tween(durationMillis = 250, easing = FastOutSlowInEasing))
                    }
                } else if (!refreshingState.value) {
                    // Released short: spring back, no reload.
                    pullOffset.animateTo(0f, tween(durationMillis = 250, easing = FastOutSlowInEasing))
                }
                return available
            }
        }
    }
    Box(modifier = modifier.nestedScroll(connection)) {
        Box(
            modifier = Modifier.offset { IntOffset(0, pullOffset.value.roundToInt()) },
        ) { content() }
        InstagramPullIndicator(
            fraction = (pullOffset.value / thresholdPx).coerceIn(0f, 1f),
            isRefreshing = isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * Core inbox screen (Phase 6).
 *
 * Gmail-familiar chrome (design.md): the top bar — drawer affordance,
 * search field and account avatar — is hoisted above the NavHost (see
 * MoPrimaryTopBar in ui/navigation), so the search pill can reshape
 * itself across tab switches; the navigation drawer (six mailbox
 * destinations: All Inbox / Primary / Promotional / Social / Spam /
 * Starred) is hoisted above the whole Scaffold in AppNavGraph (see
 * MailDrawer in this package), so it covers the top bar too.
 * Classification-backed destinations show honest empty states until
 * Phase 7 — never fabricated categories (§59).
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
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.syncUi) {
        val result = state.syncUi as? SyncUiState.Result ?: return@LaunchedEffect
        snackbar.showSnackbar(result.message)
        viewModel.consumeSyncResult()
    }

    // No drawer here: it is hoisted above the whole Scaffold in
    // AppNavGraph (see MailDrawer in this package), so it covers the
    // top bar too, exactly like Gmail's drawer — the per-screen drawer
    // only covered the content below the top bar.
    Scaffold(
            modifier = modifier,
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
                    modifier = Modifier.weight(1f),
                    header = {
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
    onRetry: () -> Boolean,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean = false,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
        // Instagram-style pull-to-refresh (2026-10-16, hardened 2026-10-17):
        // a custom pull (InstagramPullToRefresh) over the whole mailbox —
        // every state (list, empty, error, skeleton) can pull, so the
        // top-bar reload button is gone (see MoPrimaryTopBar). The
        // indicator is custom: a pill with a filling arc + "Pull to
        // refresh" / "Release to refresh" / "Refreshing…" — the
        // threshold is the signal, not a bare spinner. The pull is
        // deliberately HARD: the list travels at half the finger's
        // speed and must reach 96 dp of travel before the release
        // arms a refresh (~192 dp of finger) — a scroll-to-top swipe
        // never gets there, so it never refreshes on its own.
        val isRefreshing = state.syncUi is SyncUiState.Syncing
        // Skeleton only on the true first load (2026-10-16 fix): the
        // skeleton shows when content is Loading (nothing cached yet).
        // Once the mailbox is loaded, the ViewModel caches it, so tab
        // switches show the cached list immediately — no 300ms skeleton
        // flash on every Mail entry. (The old 300ms defer protected the
        // bubble animation, but the skeleton flash on every tab switch
        // was worse; the cached list composes fast enough.)
        InstagramPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRetry,
            modifier = modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                header()
                when (val content = state.content) {
                    // Skeleton loading (2026-10-16): while the first batch
                    // loads, show skeleton rows mirroring the real rows'
                    // geometry — never a spinner, never a layout jump.
                    MailboxContent.Loading -> MailListSkeleton(
                        modifier = Modifier.weight(1f),
                    )
                    is MailboxContent.Threads -> {
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxSize()
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
                    is MailboxContent.Messages -> {
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxSize()
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
                    is MailboxContent.Empty -> {
                        when (content.kind) {
                            EmptyKind.NO_MAIL -> MoEmptyState(
                                title = "No synchronized email yet",
                                message = "Connect a Gmail account to see your mail here. " +
                                    "Tap Sync now to fetch your messages.",
                                actionLabel = "Sync now",
                                onAction = { onRetry() },
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
                        }
                    }
                    is MailboxContent.Error -> MoErrorState(
                        message = content.message,
                        onRetry = { onRetry() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
}

/**
 * Instagram-style pull-to-refresh indicator (2026-10-16): a pill
 * with a filling arc + a threshold label, instead of Material's
 * bare spinner. "Pull to refresh" while the drag is below the
 * threshold; "Release to refresh" once past it (fraction >= 1 —
 * the release is the trigger); a spinner + "Refreshing…" while
 * the sync runs. Fades in with the pull (alpha follows the
 * fraction) so it is invisible at rest. Placed at the top of the
 * pull-to-refresh container, centered.
 */
@Composable
private fun InstagramPullIndicator(
    fraction: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = if (isRefreshing) 1f else fraction.coerceIn(0f, 1f)
    Surface(
        modifier = modifier
            .alpha(alpha)
            .padding(top = MoSpacing.sm),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MoSpacing.md,
                vertical = MoSpacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                CircularProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
            Spacer(Modifier.width(MoSpacing.sm))
            Text(
                text = when {
                    isRefreshing -> "Refreshing…"
                    fraction >= 1f -> "Release to refresh"
                    else -> "Pull to refresh"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
