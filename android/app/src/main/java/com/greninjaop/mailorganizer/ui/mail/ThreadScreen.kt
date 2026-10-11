package com.greninjaop.mailorganizer.ui.mail

import android.content.Intent
import com.greninjaop.mailorganizer.domain.write.GmailWriteOperation as WriteOp
import com.greninjaop.mailorganizer.domain.write.GmailWriteOutcome as WriteOutcome
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.rules.CorrectionSheet
import com.greninjaop.mailorganizer.ui.rules.CorrectionViewModel

/**
 * Conversation/thread view (Phase 6, phase §18–19).
 *
 * Thread ordering: oldest message first, newest last (documented Gmail
 * convention). The newest message starts expanded; older ones expand inline
 * — no separate screens just to read a thread. [focusMessageId] supports
 * deep links to a specific message (§37): it is scrolled to and expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    viewModel: ThreadViewModel,
    correctionViewModel: CorrectionViewModel,
    focusMessageId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val summaryState by viewModel.threadSummary.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var correctingMessage by remember { mutableStateOf<MessageItem?>(null) }

    // Phase 22: Gmail write confirmation + results.
    val pendingWrite by viewModel.pendingWrite.collectAsState()
    val writeResult by viewModel.writeResult.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showPermissionUpgrade by remember { mutableStateOf<PendingGmailWrite?>(null) }

    // Deep-link focus: expand the target message and scroll to it.
    LaunchedEffect(focusMessageId) {
        focusMessageId?.let { viewModel.ensureExpanded(it) }
    }

    // Phase 22: surface write outcomes once, then clear.
    LaunchedEffect(writeResult) {
        val result = writeResult as? GmailWriteResultUi.Done ?: return@LaunchedEffect
        when (val outcome = result.outcome) {
            is com.greninjaop.mailorganizer.domain.write.GmailWriteOutcome.PermissionUpgradeRequired ->
                showPermissionUpgrade = result.request
            else -> {
                val message = writeOutcomeMessage(result.request.operation, outcome)
                if (message != null) snackbar.showSnackbar(message)
            }
        }
        viewModel.clearWriteResult()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to mail")
                    }
                },
                title = {
                    Text(
                        text = MailFormatting.subjectDisplay(subject),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        when (val s = state) {
            ThreadDetailState.Loading -> MoLoadingState(
                message = "Loading thread…",
                modifier = Modifier.padding(padding),
            )
            ThreadDetailState.Empty -> MoEmptyState(
                title = "Thread not found",
                message = "This conversation may have been removed.",
                modifier = Modifier.padding(padding),
            )
            is ThreadDetailState.Content -> {
                LaunchedEffect(focusMessageId, s.messages) {
                    val target = focusMessageId ?: return@LaunchedEffect
                    val index = s.messages.indexOfFirst { it.messageId == target }
                    if (index >= 0) listState.scrollToItem(index)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    item(key = "thread_summary") {
                        ThreadSummarySection(
                            state = summaryState,
                            onRequestSummary = { viewModel.requestThreadSummary() },
                        )
                    }
                    if (s.conversation != null && s.conversation.state != com.greninjaop.mailorganizer.core.conversation.ConversationState.NO_ACTION) {
                        item(key = "conversation_intelligence") {
                            ConversationIntelligenceSection(result = s.conversation)
                        }
                    }
                    items(s.messages, key = { it.messageId }) { message ->
                        MessageCard(
                            message = message,
                            expanded = s.expandedIds.contains(message.messageId),
                            onToggleExpanded = { viewModel.toggleExpanded(message.messageId) },
                            onOpenLink = { url ->
                                openExternalLink(context, url)
                            },
                            classification = s.classifications[message.messageId],
                            priority = s.priorities[message.messageId],
                            // Phase 13: extracted meetings/deadlines.
                            temporalItems = s.temporalItems[message.messageId].orEmpty(),
                            // Phase 22: Gmail write actions (confirmation dialog
                            // owns execution; never runs directly).
                            onWriteAction = { message, operation ->
                                viewModel.requestWrite(message, operation)
                            },
                            // Phase 12: correction entry point (§4).
                            onCorrect = {
                                val classification = s.classifications[message.messageId]
                                val priority = s.priorities[message.messageId]
                                correctionViewModel.start(
                                    accountId = message.accountId,
                                    messageId = message.messageId,
                                    senderEmail = message.fromAddress,
                                    senderDomain = message.fromAddress.substringAfter('@', ""),
                                    companyName = null,
                                    currentCategory = classification?.category,
                                    currentPriority = priority?.priority,
                                    categoryOverridden = classification?.overridden == true,
                                    priorityOverridden = priority?.manualOverride == true,
                                )
                                correctingMessage = message
                            },
                        )
                    }
                }
            }
        }
    }

    // Phase 12: correction bottom sheet (§4–§6).
    if (correctingMessage != null) {
        CorrectionSheet(
            viewModel = correctionViewModel,
            onDismiss = { correctingMessage = null },
        )
    }

    // Phase 22: explicit confirmation dialog (phase §15). Confirming is the
    // only path that executes the write.
    val pending = pendingWrite
    if (pending != null) {
        GmailWriteConfirmDialog(
            pending = pending,
            onConfirm = { viewModel.confirmWrite() },
            onDismiss = { viewModel.dismissWrite() },
        )
    }

    // Phase 22: incremental-authorization upgrade (phase §9, §10).
    val upgradeRequest = showPermissionUpgrade
    if (upgradeRequest != null) {
        WritePermissionUpgradeDialog(
            accountEmail = upgradeRequest.accountEmail,
            onGrant = {
                showPermissionUpgrade = null
                try {
                    viewModel.writePermissionIntent(upgradeRequest.message.accountId)
                        ?.let { context.startActivity(it) }
                } catch (_: Exception) {
                    // Stay silent: the user can retry from the menu.
                }
            },
            onDismiss = { showPermissionUpgrade = null },
        )
    }
}

/**
 * Honest, user-safe copy for a write outcome (Phase 22, phase §25: no raw
 * errors, no tokens, no message content).
 */
private fun writeOutcomeMessage(
    operation: com.greninjaop.mailorganizer.domain.write.GmailWriteOperation,
    outcome: com.greninjaop.mailorganizer.domain.write.GmailWriteOutcome,
): String? {
    return when (outcome) {
        is WriteOutcome.Success -> if (outcome.alreadyDone) {
            "Already done."
        } else {
            when (operation) {
                WriteOp.MARK_READ -> "Marked as read."
                WriteOp.MARK_UNREAD -> "Marked as unread."
                WriteOp.STAR -> "Starred."
                WriteOp.UNSTAR -> "Star removed."
                WriteOp.ARCHIVE -> "Archived."
                WriteOp.TRASH -> "Moved to trash."
                WriteOp.UNTRASH -> "Restored from trash."
                WriteOp.ADD_LABEL -> "Label added."
                WriteOp.REMOVE_LABEL -> "Label removed."
            }
        }
        is WriteOutcome.Rejected -> outcome.reason
        is WriteOutcome.AuthRequired ->
            "Google authorization expired. Reconnect your account to continue."
        is WriteOutcome.NotFound -> "That message no longer exists in Gmail."
        is WriteOutcome.NetworkError -> outcome.message
        is WriteOutcome.Failed -> outcome.message
        // Handled separately via the upgrade dialog.
        is WriteOutcome.PermissionUpgradeRequired -> null
    }
}

/**
 * Opens a link in the external browser (phase §25). Only http/https — the
 * URL is re-validated here even though the renderer already filtered it.
 */
private fun openExternalLink(context: android.content.Context, url: String) {
    if (!com.greninjaop.mailorganizer.core.privacy.SecuritySanitizer.isSafeWebUrl(url)) return
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback for devices without a default browser handler
    }
}
