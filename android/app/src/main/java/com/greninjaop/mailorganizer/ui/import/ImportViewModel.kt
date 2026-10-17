package com.greninjaop.mailorganizer.ui.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.sync.SyncCoordinator
import com.greninjaop.mailorganizer.data.sync.SyncOutcome
import com.greninjaop.mailorganizer.data.sync.SyncProgress
import com.greninjaop.mailorganizer.data.sync.SyncTrigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Import flow ViewModel (2026-10-12).
 *
 * After a new account connects, the user is offered an explicit "Import now"
 * step. The import runs the real [SyncCoordinator.syncNow] and mirrors its
 * honest progress (stages + percentage from Gmail's resultSizeEstimate) so
 * the flying-mail animation is driven by actual work, never faked.
 */
class ImportViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val mailRepository: MailRepository,
    private val syncCoordinator: SyncCoordinator,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    sealed interface ImportUiState {
        data object Loading : ImportUiState
        data class Intro(val account: AccountRecord) : ImportUiState
        data class Importing(
            val account: AccountRecord,
            val stageText: String,
            val progress: Float?,
            val processed: Int,
            val totalEstimate: Int?,
            /**
             * The mail that just arrived, in arrival order (2026-10-17,
             * transfer card "transpiring" rows). Real rows from the
             * database — never sample data. Empty until the first batch
             * lands.
             */
            val recentMails: List<MessageRecord> = emptyList(),
        ) : ImportUiState
        data class Done(
            val account: AccountRecord,
            val message: String,
            /**
             * Last arrived mails, carried over from Importing so the
             * transfer card keeps its rows when it flips to
             * "Mailbox Organized" instead of popping empty.
             */
            val recentMails: List<MessageRecord> = emptyList(),
        ) : ImportUiState
        data class Error(val account: AccountRecord, val message: String) : ImportUiState
    }

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Loading)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private var account: AccountRecord? = null

    init {
        viewModelScope.launch(dispatchers.io) {
            val acc = accountRepository.getById(accountId)
            withContext(dispatchers.main) {
                // The account was just connected, so it must exist; if it
                // doesn't (deleted mid-flow), stay on Loading — the host
                // screen's back action exits the flow.
                if (acc != null) {
                    account = acc
                    _uiState.value = ImportUiState.Intro(acc)
                    // 2026-10-17: auto-start the import. The user just
                    // signed in to import — requiring a second tap on
                    // "Import mails" left them bouncing to the browser
                    // (the sync's auth error said "Reconnect") instead of
                    // ever seeing the transfer animation. The import runs
                    // the moment the flow appears; Back still skips it.
                    startImport()
                }
            }
        }
    }

    /**
     * "Not now": skip the animated import but still sync in the background,
     * so the inbox populates. The import flow owns the first sync (see
     * MailOrganizerApp) — skipping must not mean "never sync".
     */
    fun skipImport() {
        val acc = account ?: return
        if (_uiState.value is ImportUiState.Importing) return
        viewModelScope.launch(dispatchers.io) {
            syncCoordinator.syncNow(AccountId(accountId), SyncTrigger.MANUAL)
        }
    }

    /** Starts the real first sync; progress drives the animation. */
    fun startImport() {
        val acc = account ?: return
        if (_uiState.value is ImportUiState.Importing) return
        _uiState.value = ImportUiState.Importing(acc, "Starting import…", null, 0, null)
        viewModelScope.launch(dispatchers.io) {
            // Transpiring rows: the mail that just landed, in arrival
            // order. Collected in the same import coroutine so it stops
            // when the import finishes. Atomic `update` — the progress
            // collector below writes the same state concurrently.
            mailRepository.observeRecentlyInserted(accountId, limit = 2)
                .onEach { recent ->
                    _uiState.update { current ->
                        if (current is ImportUiState.Importing) {
                            current.copy(recentMails = recent)
                        } else {
                            current
                        }
                    }
                }
                .launchIn(this)
            syncCoordinator.progress
                .onEach { progress ->
                    if (progress is SyncProgress.Running) {
                        val total = progress.totalEstimate
                        // 100% is reserved for completion (2026-10-17
                        // fix): while the sync is still Running the
                        // fraction is capped at 99%.
                        val pct = if (total != null && total > 0) {
                            (progress.messagesProcessed.toFloat() / total).coerceIn(0f, 0.99f)
                        } else null
                        withContext(dispatchers.main) {
                            _uiState.update { current ->
                                if (current is ImportUiState.Importing) {
                                    current.copy(
                                        stageText = stageText(progress.stage),
                                        progress = pct,
                                        processed = progress.messagesProcessed,
                                        totalEstimate = total,
                                    )
                                } else {
                                    current
                                }
                            }
                        }
                    }
                }
                .launchIn(this)
            val outcome = syncCoordinator.syncNow(AccountId(accountId), SyncTrigger.MANUAL)
            withContext(dispatchers.main) {
                // Carry the last arrived mails into Done so the transfer
                // card keeps its rows on the "Mailbox Organized" flip.
                val lastRecent =
                    (_uiState.value as? ImportUiState.Importing)?.recentMails ?: emptyList()
                _uiState.value = when (outcome) {
                    is SyncOutcome.Success ->
                        ImportUiState.Done(
                            acc,
                            "Imported ${outcome.summary.messagesProcessed} messages.",
                            recentMails = lastRecent,
                        )
                    is SyncOutcome.Failed ->
                        ImportUiState.Error(acc, friendlyError(outcome.error))
                    SyncOutcome.Cancelled ->
                        ImportUiState.Intro(acc)
                }
            }
        }
    }

    private fun stageText(stage: com.greninjaop.mailorganizer.data.sync.SyncStage): String =
        when (stage) {
            com.greninjaop.mailorganizer.data.sync.SyncStage.CONNECTING -> "Connecting…"
            com.greninjaop.mailorganizer.data.sync.SyncStage.FETCHING -> "Fetching mail…"
            com.greninjaop.mailorganizer.data.sync.SyncStage.SAVING -> "Saving…"
            com.greninjaop.mailorganizer.data.sync.SyncStage.PROCESSING -> "Processing intelligence…"
            com.greninjaop.mailorganizer.data.sync.SyncStage.FINALIZING -> "Finishing…"
        }

    private fun friendlyError(error: MoError): String = when (error) {
        is MoError.Network -> "No internet connection. Your import will resume when you're back online."
        // 2026-10-17: NEVER tell the Import screen to "Reconnect your
        // account" — that sentence sent the user back to the browser
        // (Chrome) after every failed import, which is the loop they
        // reported. An auth failure here retries in place (Try again);
        // the user stays in the Import flow.
        is MoError.Authentication -> "Sign-in didn't stick. Tap Try again to retry the import."
        is MoError.RateLimited -> "Gmail is rate-limiting right now. Please try again in a bit."
        is MoError.PermissionDenied -> "Gmail permission wasn't granted. Tap Try again to retry the import."
        is MoError.Api -> "Gmail API error${error.code?.let { " ($it)" } ?: ""}. Please try again."
        is MoError.Database -> "Import failed (${error.cause?.javaClass?.simpleName ?: "storage error"}). Please try again."
        is MoError.Unexpected -> "Import failed (${error.cause?.javaClass?.simpleName ?: "unexpected error"}). Please try again."
        else -> "Import failed. Please try again."
    }
}

class ImportViewModelFactory(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val mailRepository: MailRepository,
    private val syncCoordinator: SyncCoordinator,
    private val dispatchers: AppDispatchers,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ImportViewModel(accountId, accountRepository, mailRepository, syncCoordinator, dispatchers) as T
    }
}
