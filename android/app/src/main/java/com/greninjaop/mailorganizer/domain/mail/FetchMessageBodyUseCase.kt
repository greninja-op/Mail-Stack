package com.greninjaop.mailorganizer.domain.mail

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.auth.GmailApiClient
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.withContext

/**
 * Fetch-on-open for message bodies (2026-10-13).
 *
 * The sync engine stores metadata only (`bodyText`/`bodyHtml` are null on
 * synced rows); this fetches `format=full` from Gmail on demand, parses it
 * with [com.greninjaop.mailorganizer.core.email.EmailParser], and persists
 * the body to the DB. The persisted row re-emits through the existing
 * `observeMessages` flow, so the UI updates itself — no extra plumbing.
 *
 * Failures are honest and never throw: the caller keeps its own fallback
 * UI ("No content available" stays reserved for genuinely unfetchable
 * mail). The DB row itself is the dedup signal — a successful fetch writes
 * the body, so a repeat call short-circuits.
 */
class FetchMessageBodyUseCase(
    private val api: GmailApiClient,
    private val mail: MailRepository,
    private val dispatchers: AppDispatchers,
) {
    /**
     * Ensures [messageId] has a body, fetching from Gmail when needed.
     * Returns true when the message now has body content (either it already
     * did, or the fetch + persist succeeded).
     */
    suspend fun ensureBody(accountId: AccountId, messageId: String): Boolean =
        withContext(dispatchers.io) {
            val record = mail.getMessage(messageId) ?: return@withContext false
            if (!record.bodyText.isNullOrBlank() || !record.bodyHtml.isNullOrBlank()) {
                return@withContext true
            }
            val gmailId = record.gmailMessageId ?: return@withContext false
            when (val res = api.fetchFullMessage(accountId, gmailId)) {
                is MoResult.Success -> {
                    val parsed = res.value
                    mail.updateMessageBody(messageId, parsed.bodyText, parsed.bodyHtml)
                    !parsed.bodyText.isNullOrBlank() || !parsed.bodyHtml.isNullOrBlank()
                }
                is MoResult.Failure -> {
                    MoLogger.w(TAG, "Body fetch failed (${res.error.javaClass.simpleName})")
                    false
                }
            }
        }

    private companion object {
        const val TAG = "FetchMessageBody"
    }
}
