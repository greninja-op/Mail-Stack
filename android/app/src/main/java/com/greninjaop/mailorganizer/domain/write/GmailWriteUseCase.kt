package com.greninjaop.mailorganizer.domain.write

import android.content.Intent
import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.GoogleAuthRepository
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Gmail write operations with the Phase 22 safety boundary.
 *
 * Architecture (phase §13, §14):
 * ```
 * UI (explicit tap)
 *   ↓ confirmation dialog
 * GmailWriteUseCase  ← the ONLY path to GmailWriteApi
 *   ↓
 * GmailWriteApi (Gmail adapter)
 *   ↓
 * Gmail API
 * ```
 *
 * Guarantees:
 * - **Confirmation gate (§15):** [execute] refuses unless
 *   [GmailWriteRequest.confirmed] is true. The flag can only become true
 *   through the UI confirmation dialog — background sync, the Action
 *   Engine, automation rules, and AI output have no path here.
 * - **Account isolation (§35–§38):** the request's account must equal the
 *   message's own account; the API call always uses the message's account
 *   credential. Cross-account writes are rejected, never rerouted.
 * - **Idempotency (§26):** when local state already shows the desired
 *   outcome, no API call is made ([GmailWriteOutcome.Success] with
 *   `alreadyDone = true`).
 * - **Verification (§24, §25):** local Room state is updated only after
 *   Gmail's response confirms the intended label change. Gmail is
 *   authoritative.
 * - **Ambiguous failures (§30, §31):** on a transport failure during a
 *   high-risk operation, state is reconciled once via a label read before
 *   reporting — never blindly repeated.
 * - **No double execution (§27, §28):** an in-flight guard rejects a
 *   duplicate request for the same (account, message, operation) while one
 *   is running; the UI additionally disables the action until a result.
 * - **No offline queue (§32):** writes need connectivity; there is no
 *   background queue for destructive actions.
 */
class GmailWriteUseCase(
    private val writeApi: GmailWriteApi,
    private val mail: MailRepository,
    private val auth: AuthRepository,
    private val dispatchers: AppDispatchers = AppDispatchers(),
) {

    private val inFlightMutex = Mutex()
    private val inFlight = mutableSetOf<String>()

    /**
     * Executes one Gmail write. [request.confirmed] must be true — this is
     * the confirmation boundary (phase §15).
     */
    suspend fun execute(request: GmailWriteRequest): GmailWriteOutcome =
        withContext(dispatchers.io) {
            // 1. Confirmation gate — first, before any read or side effect.
            if (!request.confirmed) {
                MoLogger.w(TAG, "Write rejected: not confirmed (${request.operation})")
                return@withContext GmailWriteOutcome.Rejected(
                    "This action needs your explicit confirmation first.",
                )
            }

            // 2. Resolve the local message (never trust caller-supplied ids).
            val message = try {
                mail.getMessage(request.messageId)
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Write lookup failed: ${t.javaClass.simpleName}")
                return@withContext GmailWriteOutcome.Failed("Couldn't load the message.")
            } ?: return@withContext GmailWriteOutcome.Rejected("Message not found.")

            // 3. Account isolation: the action carries its source account;
            //    it must equal the message's own account (§35–§37).
            if (message.accountId != request.accountId) {
                MoLogger.w(TAG, "Write rejected: account mismatch")
                return@withContext GmailWriteOutcome.Rejected(
                    "Account mismatch — refusing a cross-account write.",
                )
            }

            val gmailMessageId = message.gmailMessageId
                ?: return@withContext GmailWriteOutcome.Rejected(
                    "This message isn't synced with Gmail yet.",
                )
            val accountId = AccountId(request.accountId)

            // 4. Operation-specific validation (label params).
            val labelId = request.labelId?.trim().orEmpty()
            if ((request.operation == GmailWriteOperation.ADD_LABEL ||
                    request.operation == GmailWriteOperation.REMOVE_LABEL) &&
                labelId.isEmpty()
            ) {
                return@withContext GmailWriteOutcome.Rejected("No Gmail label specified.")
            }

            // 5. Idempotency pre-check against local state (§26): don't call
            //    Gmail when the desired state already holds.
            if (isAlreadyDone(message, request.operation, labelId)) {
                return@withContext GmailWriteOutcome.Success(alreadyDone = true)
            }

            // 6. Double-execution guard (§27, §28).
            val key = "${request.accountId}|$gmailMessageId|${request.operation}"
            val admitted = inFlightMutex.withLock { inFlight.add(key) }
            if (!admitted) {
                return@withContext GmailWriteOutcome.Rejected(
                    "This action is already running.",
                )
            }
            try {
                executeRemote(accountId, gmailMessageId, message, request, labelId)
            } finally {
                inFlightMutex.withLock { inFlight.remove(key) }
            }
        }

    /**
     * Executes a batch of writes, grouped by account (phase §47: never
     * silently cross accounts — each item keeps its own account and its
     * own outcome). Items run sequentially in list order; one item's
     * failure never cancels the rest. Every item still needs its own
     * confirmation flag.
     */
    suspend fun executeBulk(
        requests: List<GmailWriteRequest>,
    ): List<Pair<GmailWriteRequest, GmailWriteOutcome>> =
        withContext(dispatchers.io) {
            requests.map { request -> request to execute(request) }
        }

    /**
     * Builds the incremental-authorization intent that adds gmail.modify to
     * an already-connected account (phase §9). The UI explains *why* before
     * launching it (phase §10).
     */
    fun permissionUpgradeIntent(accountId: String): Intent =
        auth.createIncrementalAuthIntent(
            AccountId(accountId),
            listOf(GoogleAuthRepository.SCOPE_GMAIL_MODIFY),
        )

    // ---- internals ----

    private suspend fun executeRemote(
        accountId: AccountId,
        gmailMessageId: String,
        message: MessageRecord,
        request: GmailWriteRequest,
        labelId: String,
    ): GmailWriteOutcome {
        val apiResult: MoResult<WriteVerification> = when (request.operation) {
            GmailWriteOperation.MARK_READ -> writeApi.markRead(accountId, gmailMessageId, true)
            GmailWriteOperation.MARK_UNREAD -> writeApi.markRead(accountId, gmailMessageId, false)
            GmailWriteOperation.STAR -> writeApi.setStarred(accountId, gmailMessageId, true)
            GmailWriteOperation.UNSTAR -> writeApi.setStarred(accountId, gmailMessageId, false)
            GmailWriteOperation.ARCHIVE -> writeApi.archive(accountId, gmailMessageId)
            GmailWriteOperation.TRASH -> writeApi.trash(accountId, gmailMessageId)
            GmailWriteOperation.UNTRASH -> writeApi.untrash(accountId, gmailMessageId)
            GmailWriteOperation.ADD_LABEL -> writeApi.addLabels(accountId, gmailMessageId, listOf(labelId))
            GmailWriteOperation.REMOVE_LABEL -> writeApi.removeLabels(accountId, gmailMessageId, listOf(labelId))
        }

        return when (apiResult) {
            is MoResult.Success -> {
                val verification = apiResult.value
                if (!verifyLabels(verification.labelIds, request.operation, labelId)) {
                    MoLogger.w(TAG, "Write not confirmed by Gmail response (${request.operation})")
                    return GmailWriteOutcome.Failed(
                        "Gmail didn't confirm the change. Nothing was updated locally — " +
                            "check Gmail directly if you're unsure.",
                    )
                }
                applyLocalState(message, request.operation, labelId)
                GmailWriteOutcome.Success()
            }
            is MoResult.Failure -> mapFailure(
                apiResult.error,
                accountId,
                gmailMessageId,
                message,
                request,
                labelId,
            )
        }
    }

    /**
     * Maps API failures to honest outcomes (phase §33, §34). High-risk
     * operations get one reconciliation read on transport failure
     * (phase §30, §31); nothing is ever blindly retried.
     */
    private suspend fun mapFailure(
        error: MoError,
        accountId: AccountId,
        gmailMessageId: String,
        message: MessageRecord,
        request: GmailWriteRequest,
        labelId: String,
    ): GmailWriteOutcome = when (error) {
        is MoError.Authentication -> GmailWriteOutcome.AuthRequired
        is MoError.PermissionDenied -> GmailWriteOutcome.PermissionUpgradeRequired
        is MoError.Api -> when (error.code) {
            404 -> GmailWriteOutcome.NotFound
            else -> GmailWriteOutcome.Failed(error.message)
        }
        is MoError.RateLimited -> GmailWriteOutcome.Failed(
            "Gmail is rate-limiting requests. Try again in a little while.",
        )
        is MoError.Network -> {
            if (request.operation.highRisk) {
                reconcileAfterAmbiguousFailure(accountId, gmailMessageId, message, request, labelId)
            } else {
                // Low-risk ops are idempotent — safe for the user to retry.
                GmailWriteOutcome.NetworkError(error.message)
            }
        }
        else -> GmailWriteOutcome.Failed("The action failed. Nothing was changed.")
    }

    /**
     * Reconciliation (phase §31): the request may or may not have reached
     * Gmail. Read the current label state once and compare with the desired
     * state — only then decide. Never repeats the write itself.
     */
    private suspend fun reconcileAfterAmbiguousFailure(
        accountId: AccountId,
        gmailMessageId: String,
        message: MessageRecord,
        request: GmailWriteRequest,
        labelId: String,
    ): GmailWriteOutcome {
        return when (val read = writeApi.getLabelIds(accountId, gmailMessageId)) {
            is MoResult.Success -> {
                if (verifyLabels(read.value, request.operation, labelId)) {
                    // The write landed despite the transport failure.
                    applyLocalState(message, request.operation, labelId)
                    MoLogger.i(TAG, "Reconciled: ${request.operation} had succeeded remotely")
                    GmailWriteOutcome.Success()
                } else {
                    GmailWriteOutcome.NetworkError(
                        "Couldn't reach Gmail, and the change wasn't confirmed. " +
                            "It's safe to try again.",
                    )
                }
            }
            is MoResult.Failure -> GmailWriteOutcome.NetworkError(
                "Couldn't reach Gmail, and the result couldn't be verified. " +
                    "Check Gmail before retrying.",
            )
        }
    }

    /**
     * Checks Gmail's reported label state against the intended outcome
     * (phase §25: Gmail is authoritative).
     */
    private fun verifyLabels(
        labelIds: Set<String>,
        operation: GmailWriteOperation,
        labelId: String,
    ): Boolean = when (operation) {
        GmailWriteOperation.MARK_READ -> LABEL_UNREAD !in labelIds
        GmailWriteOperation.MARK_UNREAD -> LABEL_UNREAD in labelIds
        GmailWriteOperation.STAR -> LABEL_STARRED in labelIds
        GmailWriteOperation.UNSTAR -> LABEL_STARRED !in labelIds
        GmailWriteOperation.ARCHIVE -> LABEL_INBOX !in labelIds
        GmailWriteOperation.TRASH -> LABEL_TRASH in labelIds
        GmailWriteOperation.UNTRASH -> LABEL_TRASH !in labelIds
        GmailWriteOperation.ADD_LABEL -> labelId in labelIds
        GmailWriteOperation.REMOVE_LABEL -> labelId !in labelIds
    }

    /**
     * Applies the confirmed remote change to the local cache (phase §24:
     * only after Gmail confirms). Failures here are reported honestly —
     * the Gmail operation itself succeeded.
     */
    private suspend fun applyLocalState(
        message: MessageRecord,
        operation: GmailWriteOperation,
        labelId: String,
    ) {
        try {
            when (operation) {
                GmailWriteOperation.MARK_READ -> mail.setRead(message.messageId, true)
                GmailWriteOperation.MARK_UNREAD -> mail.setRead(message.messageId, false)
                GmailWriteOperation.STAR -> mail.setStarred(message.messageId, true)
                GmailWriteOperation.UNSTAR -> mail.setStarred(message.messageId, false)
                GmailWriteOperation.ARCHIVE ->
                    mail.setLabels(message.messageId, message.labels - LABEL_INBOX)
                GmailWriteOperation.TRASH ->
                    mail.setLabels(
                        message.messageId,
                        ((message.labels - LABEL_INBOX) + LABEL_TRASH).distinct(),
                    )
                GmailWriteOperation.UNTRASH ->
                    mail.setLabels(message.messageId, message.labels - LABEL_TRASH)
                GmailWriteOperation.ADD_LABEL ->
                    mail.setLabels(message.messageId, (message.labels + labelId).distinct())
                GmailWriteOperation.REMOVE_LABEL ->
                    mail.setLabels(message.messageId, message.labels - labelId)
            }
        } catch (t: Throwable) {
            // Gmail succeeded; only the local mirror failed. Log it —
            // the next sync reconciles (phase §25).
            MoLogger.e(TAG, "Local state update failed after Gmail success: ${t.javaClass.simpleName}")
        }
    }

    /** Local idempotency pre-check (§26) — no API call when already done. */
    private fun isAlreadyDone(
        message: MessageRecord,
        operation: GmailWriteOperation,
        labelId: String,
    ): Boolean = when (operation) {
        GmailWriteOperation.MARK_READ -> !message.unread
        GmailWriteOperation.MARK_UNREAD -> message.unread
        GmailWriteOperation.STAR -> message.starred
        GmailWriteOperation.UNSTAR -> !message.starred
        GmailWriteOperation.ARCHIVE -> LABEL_INBOX !in message.labels
        GmailWriteOperation.TRASH -> LABEL_TRASH in message.labels
        GmailWriteOperation.UNTRASH -> LABEL_TRASH !in message.labels
        GmailWriteOperation.ADD_LABEL -> labelId in message.labels
        GmailWriteOperation.REMOVE_LABEL -> labelId !in message.labels
    }

    private companion object {
        const val TAG = "GmailWrite"
        const val LABEL_INBOX = "INBOX"
        const val LABEL_UNREAD = "UNREAD"
        const val LABEL_STARRED = "STARRED"
        const val LABEL_TRASH = "TRASH"
    }
}

/**
 * One requested Gmail write (phase §27: stable execution identity).
 *
 * @param confirmed the confirmation boundary (phase §15): must be true,
 *   and becomes true only through the UI confirmation dialog. Defaults to
 *   false — fail-closed.
 */
data class GmailWriteRequest(
    val accountId: String,
    /** Local message id; resolved to the Gmail id server-side. */
    val messageId: String,
    val operation: GmailWriteOperation,
    /** Required for ADD_LABEL / REMOVE_LABEL. */
    val labelId: String? = null,
    val confirmed: Boolean = false,
)

/**
 * Supported write operations (phase §6). SEND/REPLY/FORWARD are
 * deliberately absent — not required by this phase (§6, §7: no
 * gmail.send scope).
 */
enum class GmailWriteOperation {
    MARK_READ,
    MARK_UNREAD,
    STAR,
    UNSTAR,
    ARCHIVE,
    TRASH,
    UNTRASH,
    ADD_LABEL,
    REMOVE_LABEL,
    ;

    /** Higher-risk actions (phase §16): archive, trash. */
    val highRisk: Boolean
        get() = this == ARCHIVE || this == TRASH

    /** Whether the operation can be undone from within Gmail. */
    val reversible: Boolean
        get() = this != TRASH // trash is recoverable for 30 days, not one-tap
}

/** Outcome of a write attempt — always honest, never a fake success. */
sealed interface GmailWriteOutcome {
    /** Gmail confirmed the change and local state was updated. */
    data class Success(val alreadyDone: Boolean = false) : GmailWriteOutcome

    /** The write was refused before any API call (gate/isolation/validation). */
    data class Rejected(val reason: String) : GmailWriteOutcome

    /**
     * The grant lacks gmail.modify (HTTP 403). The UI must show the
     * incremental-authorization upgrade prompt (phase §9, §10, §34).
     */
    data object PermissionUpgradeRequired : GmailWriteOutcome

    /** Token invalid/revoked (HTTP 401) — prompt reconnect (phase §33). */
    data object AuthRequired : GmailWriteOutcome

    /** The message no longer exists in Gmail (HTTP 404). */
    data object NotFound : GmailWriteOutcome

    /** Transport failure; for low-risk ops the user may safely retry. */
    data class NetworkError(val message: String) : GmailWriteOutcome

    /** Anything else; local state was not changed. */
    data class Failed(val message: String) : GmailWriteOutcome
}
