package com.greninjaop.mailorganizer.domain.write

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId

/**
 * Gmail write operations (Phase 22).
 *
 * Narrow abstraction over the Gmail API mutation endpoints. The
 * [GmailWriteUseCase] is the only caller: UI, ViewModels, the Action
 * Engine, automation rules, and background sync must never call this
 * directly — every write passes the use case's confirmation gate first
 * (phase §13–§15).
 *
 * Implementations must:
 * - obtain Bearer tokens only via AuthRepository.getValidAccessToken,
 * - never log tokens or message content,
 * - map HTTP failures honestly (401/403/404/429/5xx/network),
 * - never retry automatically.
 */
interface GmailWriteApi {

    /**
     * Adds/removes the UNREAD label.
     * @return the label state Gmail reported after the write (verification).
     */
    suspend fun markRead(
        accountId: AccountId,
        gmailMessageId: String,
        read: Boolean,
    ): MoResult<WriteVerification>

    /** Adds/removes the STARRED label. */
    suspend fun setStarred(
        accountId: AccountId,
        gmailMessageId: String,
        starred: Boolean,
    ): MoResult<WriteVerification>

    /**
     * Removes the INBOX label (phase §18: archive is *not* delete — the
     * message stays in Gmail, reachable via All Mail / search).
     */
    suspend fun archive(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<WriteVerification>

    /**
     * Moves the message to Trash (phase §17: recoverable for 30 days,
     * never permanent deletion).
     */
    suspend fun trash(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<WriteVerification>

    /** Removes the message from Trash. */
    suspend fun untrash(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<WriteVerification>

    /** Adds Gmail label ids (phase §21: account-scoped by construction). */
    suspend fun addLabels(
        accountId: AccountId,
        gmailMessageId: String,
        labelIds: List<String>,
    ): MoResult<WriteVerification>

    /** Removes Gmail label ids. */
    suspend fun removeLabels(
        accountId: AccountId,
        gmailMessageId: String,
        labelIds: List<String>,
    ): MoResult<WriteVerification>

    /**
     * Reads the current label ids for reconciliation (phase §31): after an
     * ambiguous network failure, check whether the write already landed
     * before deciding anything.
     */
    suspend fun getLabelIds(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<Set<String>>
}

/**
 * The label state Gmail reported in its response to a write — the
 * verification evidence (phase §24, §25). Gmail is authoritative; local
 * state is only updated when this confirms the intended change.
 */
data class WriteVerification(
    val gmailMessageId: String,
    val labelIds: Set<String>,
)
