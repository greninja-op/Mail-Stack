package com.greninjaop.mailorganizer.data.auth

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.JsonParser
import com.greninjaop.mailorganizer.core.JsonParser.array
import com.greninjaop.mailorganizer.core.JsonParser.long
import com.greninjaop.mailorganizer.core.JsonParser.obj
import com.greninjaop.mailorganizer.core.JsonParser.string
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.email.EmailMessage
import com.greninjaop.mailorganizer.core.email.EmailParser
import com.greninjaop.mailorganizer.core.email.RawGmailBody
import com.greninjaop.mailorganizer.core.email.RawGmailHeader
import com.greninjaop.mailorganizer.core.email.RawGmailMessage
import com.greninjaop.mailorganizer.core.email.RawGmailPart
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.TransportResponse
import com.greninjaop.mailorganizer.data.http.UrlConnectionTransport
import com.greninjaop.mailorganizer.domain.write.GmailWriteApi
import com.greninjaop.mailorganizer.domain.write.WriteVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Gmail API client (Phase 3 §26, Phase 22).
 *
 * Uses direct HTTPS calls to the Gmail REST API (no heavy Google client
 * libraries). The client is fail-closed: if no valid token is available,
 * operations return an honest "not connected" state rather than crashing.
 *
 * Phase 3 scope: Gmail API client foundation + authenticated smoke test.
 * Phase 22 scope: hardened write operations (mark read/unread, star/unstar,
 * archive, trash/untrash, label add/remove). Every write:
 * - requires the gmail.modify scope (requested incrementally, never bundled
 *   into the Phase 3 sign-in),
 * - returns the label state Gmail reported, so callers can verify the
 *   change instead of assuming it,
 * - maps HTTP failures honestly: 401 → Authentication, 403 → PermissionDenied
 *   (drives the incremental-auth upgrade prompt), 404 → Api, 429 →
 *   RateLimited, 5xx → Api, transport failures → Network,
 * - never retries: ambiguous results are reconciled by the caller (the
 *   write use case), never by blind repetition.
 *
 * Tokens come only from [AuthRepository.getValidAccessToken]. Tokens and
 * message content are never logged.
 */
class GmailApiClient(
    private val authRepository: AuthRepository,
    private val apiBase: String = "https://gmail.googleapis.com",
    private val transport: HttpTransport = UrlConnectionTransport(),
) : GmailWriteApi {

    /**
     * Performs an authenticated smoke test: fetches the Gmail profile.
     * Returns the email address on success, null on failure.
     *
     * This verifies that the OAuth grant works without syncing any mail.
     */
    suspend fun smokeTest(accountId: AccountId): SmokeTestResult {
        val accessToken = authRepository.getValidAccessToken(accountId)
            ?: return SmokeTestResult.Failure("Not connected")

        return withContext(Dispatchers.IO) {
            when (val res = get("$apiBase/gmail/v1/users/me/profile", accessToken)) {
                is ApiResponse.Ok -> {
                    val email = JsonParser.parse(res.body)?.string("emailAddress") ?: "unknown"
                    MoLogger.i(TAG, "Gmail smoke test succeeded")
                    SmokeTestResult.Success(email)
                }
                is ApiResponse.Err -> SmokeTestResult.Failure(res.error.userMessage())
            }
        }
    }

    sealed interface SmokeTestResult {
        data class Success(val email: String) : SmokeTestResult
        data class Failure(val reason: String) : SmokeTestResult
    }

    // ---- Phase 22 write operations ----

    /**
     * Marks a message as read/unread (Phase 22).
     *
     * Requires gmail.modify scope. MUST only be called after explicit
     * user action (never automatic) — enforced by GmailWriteUseCase.
     */
    override suspend fun markRead(
        accountId: AccountId,
        gmailMessageId: String,
        read: Boolean,
    ): MoResult<WriteVerification> = modifyLabels(
        accountId = accountId,
        gmailMessageId = gmailMessageId,
        addLabels = if (read) emptyList() else listOf(LABEL_UNREAD),
        removeLabels = if (read) listOf(LABEL_UNREAD) else emptyList(),
    )

    /** Backward-compatible alias kept for existing call sites. */
    suspend fun markAsRead(
        accountId: AccountId,
        messageId: String,
        isRead: Boolean,
    ): MoResult<WriteVerification> = markRead(accountId, messageId, isRead)

    /** Adds/removes the STARRED label (Phase 22). */
    override suspend fun setStarred(
        accountId: AccountId,
        gmailMessageId: String,
        starred: Boolean,
    ): MoResult<WriteVerification> = modifyLabels(
        accountId = accountId,
        gmailMessageId = gmailMessageId,
        addLabels = if (starred) listOf(LABEL_STARRED) else emptyList(),
        removeLabels = if (starred) emptyList() else listOf(LABEL_STARRED),
    )

    /**
     * Archives a message (removes INBOX label) (Phase 22).
     *
     * MUST only be called after explicit user confirmation.
     */
    override suspend fun archive(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<WriteVerification> = modifyLabels(
        accountId = accountId,
        gmailMessageId = gmailMessageId,
        addLabels = emptyList(),
        removeLabels = listOf(LABEL_INBOX),
    )

    /** Backward-compatible alias kept for existing call sites. */
    suspend fun archiveMessage(
        accountId: AccountId,
        messageId: String,
    ): MoResult<WriteVerification> = archive(accountId, messageId)

    /** Moves a message to Trash (recoverable; never permanent delete). */
    override suspend fun trash(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<WriteVerification> = withWriteToken(accountId) { token ->
        when (val res = post("$apiBase/gmail/v1/users/me/messages/$gmailMessageId/trash", token, null)) {
            is ApiResponse.Ok -> verificationFrom(res.body, gmailMessageId)
            is ApiResponse.Err -> MoResult.Failure(res.error)
        }
    }

    /** Removes a message from Trash. */
    override suspend fun untrash(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<WriteVerification> = withWriteToken(accountId) { token ->
        when (val res = post("$apiBase/gmail/v1/users/me/messages/$gmailMessageId/untrash", token, null)) {
            is ApiResponse.Ok -> verificationFrom(res.body, gmailMessageId)
            is ApiResponse.Err -> MoResult.Failure(res.error)
        }
    }

    override suspend fun addLabels(
        accountId: AccountId,
        gmailMessageId: String,
        labelIds: List<String>,
    ): MoResult<WriteVerification> {
        if (labelIds.isEmpty()) return MoResult.Failure(MoError.InvalidConfiguration("No labels to add."))
        return modifyLabels(accountId, gmailMessageId, addLabels = labelIds, removeLabels = emptyList())
    }

    override suspend fun removeLabels(
        accountId: AccountId,
        gmailMessageId: String,
        labelIds: List<String>,
    ): MoResult<WriteVerification> {
        if (labelIds.isEmpty()) return MoResult.Failure(MoError.InvalidConfiguration("No labels to remove."))
        return modifyLabels(accountId, gmailMessageId, addLabels = emptyList(), removeLabels = labelIds)
    }

    override suspend fun getLabelIds(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<Set<String>> = withWriteToken(accountId) { token ->
        when (val res = get("$apiBase/gmail/v1/users/me/messages/$gmailMessageId?format=minimal", token)) {
            is ApiResponse.Ok -> {
                val parsed = JsonParser.parse(res.body)
                    ?: return@withWriteToken MoResult.Failure(MoError.Parsing("Gmail's response couldn't be understood."))
                val labels = parsed.array("labelIds")
                    ?.mapNotNull { (it as? JsonParser.JsonValue.Str)?.value }
                    ?.toSet() ?: emptySet()
                MoResult.Success(labels)
            }
            is ApiResponse.Err -> MoResult.Failure(res.error)
        }
    }

    /**
     * Modifies message labels via Gmail API (Phase 22).
     * Returns Gmail's reported label state for verification.
     */
    private suspend fun modifyLabels(
        accountId: AccountId,
        gmailMessageId: String,
        addLabels: List<String>,
        removeLabels: List<String>,
    ): MoResult<WriteVerification> = withWriteToken(accountId) { token ->
        val body = buildString {
            append("{\"addLabelIds\":[")
            append(addLabels.joinToString(",") { jsonString(it) })
            append("],\"removeLabelIds\":[")
            append(removeLabels.joinToString(",") { jsonString(it) })
            append("]}")
        }
        when (val res = post("$apiBase/gmail/v1/users/me/messages/$gmailMessageId/modify", token, body)) {
            is ApiResponse.Ok -> verificationFrom(res.body, gmailMessageId)
            is ApiResponse.Err -> MoResult.Failure(res.error)
        }
    }

    /**
     * Runs [block] with a valid token on IO, mapping a missing token to an
     * honest Authentication failure. The token never leaves this function.
     */
    private suspend fun <T> withWriteToken(
        accountId: AccountId,
        block: suspend (token: String) -> MoResult<T>,
    ): MoResult<T> {
        val accessToken = authRepository.getValidAccessToken(accountId)
            ?: return MoResult.Failure(
                MoError.Authentication("Not connected. Connect your Google account first."),
            )
        return withContext(Dispatchers.IO) {
            try {
                block(accessToken)
            } catch (e: Exception) {
                // Never log the token or message content — only the class.
                MoLogger.e(TAG, "Gmail write failed: ${e.javaClass.simpleName}")
                MoResult.Failure(MoError.Network("Network error — couldn't reach Gmail."))
            }
        }
    }

    /** Parses the message resource Gmail returns into verification evidence. */
    private fun verificationFrom(body: String, gmailMessageId: String): MoResult<WriteVerification> {
        val parsed = JsonParser.parse(body)
            ?: run {
                MoLogger.w(TAG, "Couldn't parse Gmail write response")
                return MoResult.Failure(MoError.Parsing("Gmail's response couldn't be understood."))
            }
        val labels = parsed.array("labelIds")
            ?.mapNotNull { (it as? JsonParser.JsonValue.Str)?.value }
            ?.toSet() ?: emptySet()
        return MoResult.Success(WriteVerification(gmailMessageId, labels))
    }

    /** Minimal JSON string escaper for label ids (no new dependencies). */
    private fun jsonString(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    // ---- HTTP plumbing ----

    private sealed interface ApiResponse {
        data class Ok(val body: String) : ApiResponse
        data class Err(val error: MoError) : ApiResponse
    }

    private fun get(url: String, token: String): ApiResponse =
        request(url, token, method = "GET", body = null)

    private fun post(url: String, token: String, body: String?): ApiResponse =
        request(url, token, method = "POST", body = body)

    private fun request(url: String, token: String, method: String, body: String?): ApiResponse {
        return try {
            val headers = mutableMapOf(
                // The token is set as a header value; never logged or stored.
                "Authorization" to "Bearer $token",
            )
            if (body != null) {
                headers["Content-Type"] = "application/json; charset=utf-8"
            }
            val response: TransportResponse = transport.execute(method, url, headers, body)
            when (response.code) {
                200 -> ApiResponse.Ok(response.body)
                // The error body is never surfaced (phase §25).
                else -> ApiResponse.Err(mapHttpError(response.code))
            }
        } catch (e: IOException) {
            // Transport failure (DNS, TLS, timeout, refused). The caller
            // decides whether reconciliation is needed — never retried here.
            ApiResponse.Err(MoError.Network("Couldn't reach Gmail: ${e.message?.take(80)}"))
        }
    }

    /**
     * Honest HTTP error mapping (phase §33, §34). 403 specifically means
     * the grant lacks gmail.modify → the use case turns it into the
     * incremental-authorization upgrade prompt (phase §9).
     */
    /**
     * Fetches one message's full MIME payload (`format=full`) and parses it
     * into the canonical [EmailMessage].
     *
     * Fetch-on-open (2026-10-13): the sync stores metadata only
     * (`bodyText`/`bodyHtml` null); bodies arrive when the user opens a
     * thread. Fail-closed like every other client method: no token means an
     * honest Authentication failure, never a crash. Tokens and message
     * content are never logged.
     */
    suspend fun fetchFullMessage(
        accountId: AccountId,
        gmailMessageId: String,
    ): MoResult<EmailMessage> = withContext(Dispatchers.IO) {
        val accessToken = authRepository.getValidAccessToken(accountId)
            ?: return@withContext MoResult.Failure(
                MoError.Authentication("Not connected to Gmail. Reconnect your account."),
            )
        when (val res = get("$apiBase/gmail/v1/users/me/messages/$gmailMessageId?format=full", accessToken)) {
            is ApiResponse.Ok -> {
                val raw = parseRawFullMessage(res.body, gmailMessageId)
                    ?: return@withContext MoResult.Failure(
                        MoError.Parsing("Gmail's response couldn't be understood."),
                    )
                MoResult.Success(EmailParser.parse(raw))
            }
            is ApiResponse.Err -> MoResult.Failure(res.error)
        }
    }

    /**
     * Builds a [RawGmailMessage] from a `format=full` response. Total:
     * malformed parts degrade to null and the parser degrades gracefully —
     * one bad part never breaks the fetch.
     */
    private fun parseRawFullMessage(body: String, fallbackId: String): RawGmailMessage? {
        val json = JsonParser.parse(body) as? JsonParser.JsonValue.Obj ?: return null
        fun parsePart(value: JsonParser.JsonValue): RawGmailPart? {
            val o = value as? JsonParser.JsonValue.Obj ?: return null
            val bodyObj = o.obj("body")
            return RawGmailPart(
                mimeType = o.string("mimeType").orEmpty(),
                filename = o.string("filename")?.takeIf { it.isNotEmpty() },
                headers = o.array("headers").orEmpty().mapNotNull { h ->
                    val ho = h as? JsonParser.JsonValue.Obj ?: return@mapNotNull null
                    val name = ho.string("name") ?: return@mapNotNull null
                    val headerValue = ho.string("value") ?: return@mapNotNull null
                    RawGmailHeader(name, headerValue)
                },
                body = bodyObj?.let {
                    RawGmailBody(
                        data = it.string("data"),
                        attachmentId = it.string("attachmentId"),
                        size = it.long("size") ?: 0L,
                    )
                },
                parts = o.array("parts").orEmpty().mapNotNull(::parsePart),
            )
        }
        return RawGmailMessage(
            id = json.string("id") ?: fallbackId,
            threadId = json.string("threadId").orEmpty(),
            labelIds = json.array("labelIds").orEmpty()
                .mapNotNull { (it as? JsonParser.JsonValue.Str)?.value },
            snippet = json.string("snippet"),
            internalDateMs = json.string("internalDate")?.toLongOrNull()
                ?: json.long("internalDate"),
            sizeEstimate = json.string("sizeEstimate")?.toLongOrNull()
                ?: json.long("sizeEstimate"),
            payload = json.obj("payload")?.let(::parsePart),
        )
    }

    private fun mapHttpError(code: Int): MoError {
        MoLogger.w(TAG, "Gmail API error: HTTP $code")
        return when (code) {
            401 -> MoError.Authentication(
                "Google authorization expired or was revoked. Reconnect your account.",
            )
            403 -> MoError.PermissionDenied(
                "gmail.modify",
            )
            404 -> MoError.Api(
                404,
                "That message no longer exists in Gmail.",
            )
            429 -> MoError.RateLimited(retryAfterSeconds = null)
            in 500..599 -> MoError.Api(
                code,
                "Gmail had a problem (HTTP $code). Nothing was changed locally.",
            )
            else -> MoError.Api(
                code,
                "Gmail request failed (HTTP $code).",
            )
        }
    }

    /**
     * User-safe copy for a mapped error. Never includes tokens, message
     * content, or raw server payloads (phase §25).
     */
    private fun MoError.userMessage(): String = when (this) {
        is MoError.Authentication -> message
        is MoError.PermissionDenied -> "Additional Gmail permission is needed for this action."
        is MoError.Api -> message
        is MoError.RateLimited -> "Gmail is rate-limiting requests. Try again in a little while."
        is MoError.Network -> message
        else -> "Couldn't reach Gmail."
    }

    companion object {
        private const val TAG = "GmailApiClient"
        const val LABEL_INBOX = "INBOX"
        const val LABEL_UNREAD = "UNREAD"
        const val LABEL_STARRED = "STARRED"
        const val LABEL_TRASH = "TRASH"
    }
}
