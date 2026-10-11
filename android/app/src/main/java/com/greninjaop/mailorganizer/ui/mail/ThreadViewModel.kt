package com.greninjaop.mailorganizer.ui.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.temporal.ExtractedTemporal
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.domain.temporal.toExtractedTemporal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.greninjaop.mailorganizer.core.conversation.ConversationAnalysisResult
import com.greninjaop.mailorganizer.domain.conversation.ConversationIntelligenceUseCase

/**
 * Backs the Phase 6 thread/conversation screen.
 *
 * Thread ordering (phase §51): messages are shown oldest-first, newest last
 * — the Gmail conversation convention. The newest message starts expanded;
 * older messages are collapsed and expand inline (§19: no separate screens
 * just to read a thread). Account scope is inherited from the thread row
 * itself (threads are account-scoped by schema, §33).
 */
class ThreadViewModel @JvmOverloads constructor(
    private val threadId: String,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
    private val conversationIntelligence: ConversationIntelligenceUseCase? = null,
    private val aiFallback: com.greninjaop.mailorganizer.domain.ai.AiFallbackUseCase? = null,
    /**
     * Phase 22: Gmail write operations. Null when writes are unavailable
     * (e.g. tests) — the UI then hides write actions entirely.
     */
    private val gmailWrite: com.greninjaop.mailorganizer.domain.write.GmailWriteUseCase? = null,
    /**
     * Phase 22: resolves the account email for the confirmation dialog so
     * the user sees *which* account a write targets (phase §46).
     */
    private val accounts: com.greninjaop.mailorganizer.data.repository.AccountRepository? = null,
) : ViewModel() {

    private val expandedIds = MutableStateFlow<Set<String>>(emptySet())
    private var defaultExpanded = false

    private val messages = mail.observeMessages(threadId, MAX_THREAD_MESSAGES)
        .map { records -> records.map { it.toMessageItem() }.inThreadOrder() }
        .onEach { items ->
            // Default: the newest message starts expanded (§19).
            if (!defaultExpanded && items.isNotEmpty()) {
                defaultExpanded = true
                expandedIds.value = setOf(items.last().messageId)
            }
        }
        .catch { t ->
            MoLogger.e(TAG, "Thread load failed: ${t.javaClass.simpleName}")
            emit(emptyList())
        }

    /**
     * Phase 7: classification per message (batch lookup, no N+1).
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val classifications =
        messages.mapLatest { items ->
            try {
                val byId = intelligence.getClassifications(items.map { it.messageId })
                items.associate { it.messageId to byId[it.messageId] }
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Classification lookup failed: ${t.javaClass.simpleName}")
                items.associate { it.messageId to null }
            }
        }

    /**
     * Phase 9: priority per message (message-level evidence preserved).
     * Threads are bounded (200), so one batch lookup per visible thread
     * is fine; failures degrade to "no priority shown".
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val priorities =
        messages.mapLatest { items ->
            try {
                intelligence.getPriorities(items.map { it.messageId })
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Priority lookup failed: ${t.javaClass.simpleName}")
                emptyMap()
            }
        }

    /**
     * Phase 13: temporal items per message (batch lookup, no N+1).
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val temporalItems =
        messages.mapLatest { items ->
            try {
                val extracted = intelligence.getExtractedItemsByMessages(items.map { it.messageId })
                val byMessage = extracted.groupBy { it.messageId }
                items.associate { item ->
                    item.messageId to (byMessage[item.messageId]?.mapNotNull { it.toExtractedTemporal() } ?: emptyList())
                }
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Temporal lookup failed: ${t.javaClass.simpleName}")
                emptyMap()
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val conversationState =
        messages.mapLatest { items ->
            if (conversationIntelligence != null && items.isNotEmpty()) {
                try {
                    conversationIntelligence.analyzeThread(threadId)
                } catch (t: Throwable) {
                    MoLogger.w(TAG, "Conversation analysis failed: ${t.javaClass.simpleName}")
                    null
                }
            } else {
                null
            }
        }

    private val baseState = combine(
        messages,
        expandedIds,
        classifications,
        priorities,
        temporalItems,
    ) { items, expanded, classById, prioById, temporalById ->
        if (items.isEmpty()) {
            ThreadDetailState.Empty
        } else {
            ThreadDetailState.Content(
                messages = items,
                expandedIds = expanded,
                classifications = classById,
                priorities = prioById,
                temporalItems = temporalById,
                conversation = null,
            )
        }
    }

    val state: StateFlow<ThreadDetailState> = combine(
        baseState,
        conversationState,
    ) { base, conv ->
        if (base is ThreadDetailState.Content) {
            base.copy(conversation = conv)
        } else {
            base
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThreadDetailState.Loading,
    )

    /** Subject for the top bar: threads share one subject; derived from messages. */
    val subject: StateFlow<String> = messages
        .map { items -> items.firstOrNull()?.subject.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun toggleExpanded(messageId: String) {
        val current = expandedIds.value
        expandedIds.value =
            if (current.contains(messageId)) current - messageId else current + messageId
    }

    /** Deep-link support (§37): ensure a specific message is expanded. */
    fun ensureExpanded(messageId: String) {
        if (!expandedIds.value.contains(messageId)) {
            expandedIds.value = expandedIds.value + messageId
        }
    }

    private val _threadSummary = MutableStateFlow<ThreadSummaryUiState>(ThreadSummaryUiState.Idle)
    /** On-demand, user-initiated thread summary (Phase 26 §49, §57). */
    val threadSummary: StateFlow<ThreadSummaryUiState> = _threadSummary

    /** Requests AI thread summarization on user demand (§49, §100). */
    fun requestThreadSummary() {
        val ai = aiFallback ?: run {
            _threadSummary.value = ThreadSummaryUiState.Unavailable
            return
        }
        viewModelScope.launch(dispatchers.main) {
            _threadSummary.value = ThreadSummaryUiState.Loading
            try {
                val msgs = mail.observeMessages(threadId, MAX_THREAD_MESSAGES).first()
                val accountId = msgs.firstOrNull()?.accountId.orEmpty()
                if (accountId.isEmpty()) {
                    _threadSummary.value = ThreadSummaryUiState.Error("Unable to identify thread account")
                    return@launch
                }
                when (val res = ai.summarizeThread(accountId, threadId)) {
                    is com.greninjaop.mailorganizer.MoResult.Success -> {
                        _threadSummary.value = ThreadSummaryUiState.Content(
                            summary = res.value.output.summary,
                            keyPoints = res.value.output.keyPoints,
                            confidence = res.value.output.confidence,
                        )
                    }
                    is com.greninjaop.mailorganizer.MoResult.Failure -> {
                        _threadSummary.value = ThreadSummaryUiState.Error("AI summarization is currently unavailable or disabled")
                    }
                }
            } catch (t: Throwable) {
                MoLogger.w(TAG, "Thread summary request failed: ${t.javaClass.simpleName}")
                _threadSummary.value = ThreadSummaryUiState.Error("Unable to generate summary")
            }
        }
    }

    // ---- Phase 22: Gmail write actions (explicit user confirmation) ----

    /**
     * Whether write actions are available for [message]: the write use case
     * must be present and the message must be synced with Gmail (fixture /
     * unsynced rows get no write affordances — never a fake action).
     */
    fun canWrite(message: MessageItem): Boolean =
        gmailWrite != null && message.syncedWithGmail

    private val _pendingWrite =
        MutableStateFlow<PendingGmailWrite?>(null)

    /**
     * The write awaiting explicit user confirmation. The confirmation
     * dialog (phase §15) renders this; nothing executes until
     * [confirmWrite] is called from the dialog's confirm button.
     */
    val pendingWrite: StateFlow<PendingGmailWrite?> = _pendingWrite

    private val _writeResult = MutableStateFlow<GmailWriteResultUi?>(null)

    /** Result of the last write attempt; the UI shows it once, then clears. */
    val writeResult: StateFlow<GmailWriteResultUi?> = _writeResult

    /** Opens the confirmation dialog for [operation] on [message]. */
    fun requestWrite(message: MessageItem, operation: com.greninjaop.mailorganizer.domain.write.GmailWriteOperation) {
        if (!canWrite(message)) {
            MoLogger.w(TAG, "Write action unavailable for message ${message.messageId}")
            return
        }
        // Double-tap protection (§28): ignore while one is executing.
        if (_writeResult.value is GmailWriteResultUi.Executing) return
        _writeResult.value = null
        viewModelScope.launch(dispatchers.io) {
            val email = try {
                accounts?.getById(message.accountId)?.emailAddress
            } catch (t: Throwable) {
                MoLogger.w(TAG, "Account lookup failed: ${t.javaClass.simpleName}")
                null
            }
            _pendingWrite.value = PendingGmailWrite(message, operation, email)
        }
    }

    /** Dismisses the confirmation dialog — nothing happens. */
    fun dismissWrite() {
        _pendingWrite.value = null
    }

    /**
     * Executes the pending write with the confirmation flag set (phase
     * §15). Called only from the confirmation dialog's confirm button.
     */
    fun confirmWrite() {
        val pending = _pendingWrite.value ?: return
        val useCase = gmailWrite ?: return
        _pendingWrite.value = null
        _writeResult.value = GmailWriteResultUi.Executing
        viewModelScope.launch(dispatchers.io) {
            val outcome = useCase.execute(
                com.greninjaop.mailorganizer.domain.write.GmailWriteRequest(
                    accountId = pending.message.accountId,
                    messageId = pending.message.messageId,
                    operation = pending.operation,
                    confirmed = true,
                ),
            )
            _writeResult.value = GmailWriteResultUi.Done(pending, outcome)
        }
    }

    /** Clears the last result after the UI has shown it. */
    fun clearWriteResult() {
        _writeResult.value = null
    }

    /**
     * Builds the incremental-authorization intent for the write permission
     * upgrade (phase §9). Null when the auth repository is unavailable.
     */
    fun writePermissionIntent(accountId: String): android.content.Intent? =
        try {
            gmailWrite?.permissionUpgradeIntent(accountId)
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Permission intent failed: ${t.javaClass.simpleName}")
            null
        }

    private companion object {
        const val TAG = "ThreadViewModel"
        /** Threads are bounded; Gmail threads rarely exceed this (§17). */
        const val MAX_THREAD_MESSAGES = 200
    }
}

/** A write the user tapped, awaiting explicit confirmation (phase §15). */
data class PendingGmailWrite(
    val message: MessageItem,
    val operation: com.greninjaop.mailorganizer.domain.write.GmailWriteOperation,
    /** Account email for the dialog; null when it couldn't be resolved. */
    val accountEmail: String? = null,
)

/** UI-visible write execution state (phase §28: Executing disables the action). */
sealed interface GmailWriteResultUi {
    data object Executing : GmailWriteResultUi
    data class Done(
        val request: PendingGmailWrite,
        val outcome: com.greninjaop.mailorganizer.domain.write.GmailWriteOutcome,
    ) : GmailWriteResultUi
}

/** Explicit thread-screen state (phase §35). */
sealed interface ThreadDetailState {
    data object Loading : ThreadDetailState
    data class Content(
        val messages: List<MessageItem>,
        val expandedIds: Set<String>,
        /** Message-level classifications by message id (Phase 7). */
        val classifications: Map<String, ClassificationRecord?> = emptyMap(),
        /** Message-level priorities by message id (Phase 9). */
        val priorities: Map<String, PriorityRecord?> = emptyMap(),
        /** Message-level temporal items by message id (Phase 13). */
        val temporalItems: Map<String, List<ExtractedTemporal>> = emptyMap(),
        /** Thread-level conversation analysis result (Phase 21). */
        val conversation: ConversationAnalysisResult? = null,
    ) : ThreadDetailState
    data object Empty : ThreadDetailState
}

/** State for user-initiated on-demand thread summarization (Phase 26 §49, §57). */
sealed interface ThreadSummaryUiState {
    data object Idle : ThreadSummaryUiState
    data object Loading : ThreadSummaryUiState
    data class Content(
        val summary: String,
        val keyPoints: List<String>,
        val confidence: Float,
    ) : ThreadSummaryUiState
    data class Error(val message: String) : ThreadSummaryUiState
    data object Unavailable : ThreadSummaryUiState
}

