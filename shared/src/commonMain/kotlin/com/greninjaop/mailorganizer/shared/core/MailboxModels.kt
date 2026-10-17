package com.greninjaop.mailorganizer.shared.core

/** Gmail-familiar destinations. Company filters live INSIDE a category. */
enum class MailDestination {
    ALL_INBOX,
    PRIMARY,
    PROMOTIONAL,
    SOCIAL,
    SPAM,
    STARRED,
}

enum class SyncStage {
    IDLE,
    CONNECTING,
    FETCHING,
    SAVING,
    PROCESSING,
    FINALIZING,
    SUCCEEDED,
    FAILED,
}

/**
 * Sync progress contract (Android 1.0.35+):
 * - totalEstimate is fixed once known (never revised mid-run — avoids
 *   backwards progress bars).
 * - UI caps displayed fraction at 0.99 while running; Done = 1.0.
 */
sealed class SyncProgress {
    data object Idle : SyncProgress()
    data class Running(
        val stage: SyncStage,
        val messagesProcessed: Int,
        val totalEstimate: Int? = null,
    ) : SyncProgress()
    data class Succeeded(val messagesProcessed: Int) : SyncProgress()
    data class Failed(val error: MoError) : SyncProgress()
}

data class ThreadSummary(
    val threadId: String,
    val accountId: AccountId,
    val subject: String,
    val snippet: String,
    val senderName: String,
    val senderAddress: String,
    val senderDomain: String?,
    val timestampEpochMs: Long,
    val unread: Boolean,
    val starred: Boolean,
    val accountBadgeLabel: String?,
)

data class MessageSummary(
    val messageId: String,
    val threadId: String,
    val accountId: AccountId,
    val subject: String,
    val snippet: String,
    val fromName: String?,
    val fromAddress: String,
    val senderDomain: String?,
    val timestampEpochMs: Long,
    val unread: Boolean,
    val starred: Boolean,
    val bodyText: String? = null,
    val bodyHtml: String? = null,
)

enum class CategoryGroup {
    NEEDS_ATTENTION,
    PROMOTIONS,
    UPDATES,
    WORK_AND_LEARNING,
    EVERYTHING_ELSE,
}
