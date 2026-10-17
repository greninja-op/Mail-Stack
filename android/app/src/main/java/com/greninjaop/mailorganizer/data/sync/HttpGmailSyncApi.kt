package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.JsonParser
import com.greninjaop.mailorganizer.core.JsonParser.array
import com.greninjaop.mailorganizer.core.JsonParser.long
import com.greninjaop.mailorganizer.core.JsonParser.obj
import com.greninjaop.mailorganizer.core.JsonParser.string
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.UrlConnectionTransport
import java.net.URLEncoder

/**
 * Real [GmailSyncApi] over HTTPS (Phase 4's deferred half, completed with
 * Phase 3's OAuth).
 *
 * - Bearer tokens come ONLY from [AuthRepository.getValidAccessToken]
 *   (auto-refreshing). A null token means "not connected" →
 *   [SyncApiException.NotConfigured] (fail-closed, never fake data).
 * - `format=metadata` with a minimal header set: bodies are NOT fetched
 *   during sync (phase §14 minimum viable data). Snippet + headers are
 *   enough for the local model; full bodies arrive on open if needed.
 * - Raw tokens and message content are never logged.
 *
 * [apiBase] is injectable for unit tests (local fake server).
 */
class HttpGmailSyncApi(
    private val authRepository: AuthRepository,
    private val apiBase: String = "https://gmail.googleapis.com",
    private val transport: HttpTransport = UrlConnectionTransport(),
) : GmailSyncApi {

    /**
     * Bounded parallelism for per-message metadata fetches (2026-10-12).
     * 8 concurrent `messages.get` calls ≈ 40 quota units per burst — far
     * inside Gmail's per-user limits — while cutting first-sync time from
     * ~5 minutes to well under 2.
     */
    private val fetchDispatcher = Dispatchers.IO.limitedParallelism(FETCH_CONCURRENCY)

    override suspend fun fetchPage(
        accountId: AccountId,
        pageToken: String?,
        pageSize: Int,
    ): MessagePage = fetchLabeledPage(accountId, pageToken, pageSize, labelId = null)

    override suspend fun fetchSpamPage(
        accountId: AccountId,
        pageSize: Int,
    ): MessagePage = fetchLabeledPage(accountId, pageToken = null, pageSize, labelId = "SPAM")

    /**
     * Shared list+metadata fetch. [labelId] null = the default INBOX list;
     * "SPAM" = the spam-only pass the Spam destination needs (2026-10-13).
     */
    private suspend fun fetchLabeledPage(
        accountId: AccountId,
        pageToken: String?,
        pageSize: Int,
        labelId: String?,
    ): MessagePage = withContext(Dispatchers.IO) {
        val token = requireToken(accountId)
        val capped = pageSize.coerceIn(1, MAX_PAGE_SIZE)
        val params = mutableMapOf(
            "maxResults" to capped.toString(),
            // Sync the primary mailbox view. Label filtering for
            // Promotional/Social/Spam happens locally (Phase 7/8).
        )
        if (labelId != null) params["labelIds"] = labelId
        if (pageToken != null) params["pageToken"] = pageToken
        val listJson = get(token, "$apiBase/gmail/v1/users/me/messages", params)
            ?: throw SyncApiException.NetworkError()

        val messages = listJson.array("messages").orEmpty().mapNotNull { item ->
            val id = item.string("id")
            val threadId = item.string("threadId")
            if (id != null && threadId != null) id to threadId else null
        }
        val nextPageToken = listJson.string("nextPageToken")
        // Honest progress total: Gmail's estimate of messages matching the query.
        val resultSizeEstimate = listJson.long("resultSizeEstimate")?.toInt()

        // Fetch metadata for each message with bounded concurrency
        // (2026-10-12): sequential fetching made a 500-message first sync
        // take ~5 minutes; 8 parallel requests bring it under ~2 minutes
        // while staying far inside Gmail's per-user quota.
        val remote = fetchMetadataConcurrently(token, messages, "Skipping message")
        MessagePage(
            messages = remote,
            nextPageToken = nextPageToken,
            resultSizeEstimate = resultSizeEstimate,
        )
    }

    override suspend fun fetchChanges(
        accountId: AccountId,
        historyCursor: String,
    ): ChangePage = withContext(Dispatchers.IO) {
        val token = requireToken(accountId)
        // Gmail accepts repeated historyTypes params; buildQuery handles
        // only single values, so the URL is assembled manually here.
        val query = "startHistoryId=${enc(historyCursor)}" +
            "&historyTypes=messageAdded" +
            "&historyTypes=messageDeleted" +
            "&historyTypes=labelAdded" +
            "&historyTypes=labelRemoved"
        val json = getRaw(token, "$apiBase/gmail/v1/users/me/history?$query")
            ?: throw SyncApiException.NetworkError()

        val addedIds = mutableListOf<Pair<String, String>>()
        val deletedIds = mutableListOf<String>()
        for (record in json.array("history").orEmpty()) {
            for (added in record.array("messagesAdded").orEmpty()) {
                val message = added.obj("message")
                val id = message?.string("id")
                val threadId = message?.string("threadId")
                if (id != null && threadId != null) addedIds.add(id to threadId)
            }
            for (deleted in record.array("messagesDeleted").orEmpty()) {
                deleted.obj("message")?.string("id")?.let { deletedIds.add(it) }
            }
            // labelAdded/labelRemoved carry full messages too — treat as
            // changed (re-fetch) so local unread/starred/labels stay true.
            for (key in listOf("labelsAdded", "labelsRemoved")) {
                for (entry in record.array(key).orEmpty()) {
                    val message = entry.obj("message")
                    val id = message?.string("id")
                    val threadId = message?.string("threadId")
                    if (id != null && threadId != null && addedIds.none { it.first == id }) {
                        addedIds.add(id to threadId)
                    }
                }
            }
        }
        val remote = fetchMetadataConcurrently(token, addedIds, "Skipping changed message")
        ChangePage(
            messages = remote,
            deletedRemoteIds = deletedIds.distinct(),
            nextHistoryCursor = json.string("historyId") ?: historyCursor,
        )
    }

    /**
     * Fetches per-message metadata with bounded concurrency (2026-10-12).
     * A single message failing (SyncApiException) is skipped and logged —
     * 404s (deleted between list and get) must not kill the whole batch.
     * Non-API throwables propagate: they indicate bugs, not bad messages.
     */
    private suspend fun fetchMetadataConcurrently(
        token: String,
        ids: List<Pair<String, String>>,
        skipLogPrefix: String,
    ): List<RemoteMessage> = coroutineScope {
        ids.map { (id, threadId) ->
            async(fetchDispatcher) {
                try {
                    fetchMessageMetadata(token, id, threadId)
                } catch (e: SyncApiException) {
                    MoLogger.w(TAG, "$skipLogPrefix $id: ${e.javaClass.simpleName}")
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun latestHistoryCursor(accountId: AccountId): String =
        withContext(Dispatchers.IO) {
            val token = requireToken(accountId)
            val json = get(token, "$apiBase/gmail/v1/users/me/profile", emptyMap())
                ?: throw SyncApiException.NetworkError()
            json.string("historyId") ?: throw SyncApiException.InvalidRequest(
                "Gmail profile response missing historyId",
            )
        }

    /**
     * Mailbox total from the Gmail profile (same GET as
     * [latestHistoryCursor]). Best-effort: null on any failure — the caller
     * falls back to the first page's estimate. Never throws for a missing
     * field (a profile without messagesTotal is not a sync failure).
     */
    override suspend fun mailboxTotal(accountId: AccountId): Int? =
        withContext(Dispatchers.IO) {
            try {
                val token = requireToken(accountId)
                val json = get(token, "$apiBase/gmail/v1/users/me/profile", emptyMap())
                    ?: return@withContext null
                json.long("messagesTotal")?.toInt()
            } catch (t: Throwable) {
                MoLogger.w("HttpGmailSyncApi", "mailboxTotal unavailable (${t.javaClass.simpleName})")
                null
            }
        }

    private suspend fun requireToken(accountId: AccountId): String =
        authRepository.getValidAccessToken(accountId)
            ?: throw SyncApiException.NotConfigured()

    /**
     * Fetches one message in metadata format and maps it to [RemoteMessage].
     * Pure mapping is in [parseMetadataMessage] (unit-tested).
     */
    private fun fetchMessageMetadata(
        token: String,
        id: String,
        threadId: String,
    ): RemoteMessage {
        // Repeated metadataHeaders params: assemble manually like history.
        val query = "format=metadata" +
            "&metadataHeaders=From&metadataHeaders=To&metadataHeaders=Cc" +
            "&metadataHeaders=Subject&metadataHeaders=Date"
        val json = getRaw(token, "$apiBase/gmail/v1/users/me/messages/$id?$query")
            ?: throw SyncApiException.NetworkError()
        return parseMetadataMessage(json, id, threadId)
            ?: throw SyncApiException.InvalidRequest("Unparseable message $id")
    }

    // ---------- HTTP ----------

    private fun get(
        token: String,
        url: String,
        params: Map<String, String>,
    ): JsonParser.JsonValue? {
        val fullUrl = if (params.isEmpty()) url else "$url?${buildQuery(params)}"
        return getRaw(token, fullUrl)
    }

    private fun getRaw(token: String, fullUrl: String): JsonParser.JsonValue? {
        val response = try {
            transport.execute(
                method = "GET",
                url = fullUrl,
                headers = mapOf(
                    "Authorization" to "Bearer $token",
                    "Accept" to "application/json",
                ),
                body = null,
            )
        } catch (e: IOException) {
            throw SyncApiException.NetworkError(e)
        }
        val code = response.code
        if (code != 200) {
            handleErrorCode(code, response.body, fullUrl)
        }
        return JsonParser.parse(response.body)
            ?: throw SyncApiException.InvalidRequest("Invalid JSON from Gmail")
    }

    /** Maps non-200 responses to [SyncApiException] subclasses. Never throws. */
    private fun handleErrorCode(
        code: Int,
        body: String,
        url: String,
    ): Nothing {
        // Parse the FULL body (2026-10-17 fix): the old code did
        // body.take(500) BEFORE parsing, which truncated long Gmail
        // errors mid-JSON (e.g. accessNotConfigured, whose message +
        // extendedHelp URL run well past 500 chars). A truncated body
        // is invalid JSON, so the parse failed and reason/message came
        // back empty — which is why the Import showed the generic
        // "Gmail refused the request" instead of Gmail's actual reason.
        val errorObj = JsonParser.parse(body)?.obj("error")
        // Gmail's reason lives in error.errors[0].reason, NOT error.reason
        // (2026-10-17 fix: the old code read error.reason, which is always
        // absent, so every 403 — including rate limits — was misclassified
        // as PermissionDenied and the real reason was lost).
        val reason = errorObj?.array("errors")?.firstOrNull()?.string("reason").orEmpty()
        val message = errorObj?.string("message").orEmpty()
            .ifEmpty { errorObj?.array("errors")?.firstOrNull()?.string("message").orEmpty() }
        // Log the full body (bounded) so the next 403 can be diagnosed
        // from the log even if the parse fails.
        MoLogger.w(
            TAG,
            "Gmail API HTTP $code reason=$reason url=${url.substringBefore("?")} " +
                "body=${body.take(2000)}",
        )
        throw when {
            code == 401 -> SyncApiException.AuthExpired()
            code == 404 && url.contains("/history?") -> SyncApiException.HistoryInvalid()
            code == 404 -> SyncApiException.InvalidRequest("Not found: $message".take(120))
            code == 429 -> SyncApiException.RateLimited(null)
            code == 403 && reason.contains("RateLimitExceeded", ignoreCase = true) ->
                SyncApiException.RateLimited(null)
            code == 403 && reason.contains("UserRateLimitExceeded", ignoreCase = true) ->
                SyncApiException.RateLimited(null)
            // Daily quota (Gmail returns this, not UserRateLimitExceeded,
            // when the project's daily quota is exhausted).
            code == 403 && reason.equals("dailyLimitExceeded", ignoreCase = true) ->
                SyncApiException.RateLimited(null)
            code == 403 -> SyncApiException.PermissionDenied(
                detail = permissionDeniedDetail(reason, message),
            )
            code in 500..599 -> SyncApiException.ServerError(code)
            else -> SyncApiException.InvalidRequest("Gmail API HTTP $code".take(120))
        }
    }

    /**
     * Human-readable 403 detail, so the Import error tells the user WHY
     * Gmail refused instead of the opaque "permission wasn't granted"
     * (which retry can never fix). The reason decides the remedy:
     * insufficientPermissions = the grant lacks gmail.readonly (re-grant);
     * accessNotConfigured = the Gmail API isn't enabled for the project
     * (Cloud Console); anything else = Gmail's own message.
     */
    private fun permissionDeniedDetail(reason: String, message: String): String {
        val cleanMessage = message.take(140).ifEmpty { "Gmail refused the request" }
        return when {
            // Gmail's scope error is "insufficientScopes" (not
            // "insufficientPermissions") — both mean the grant lacks
            // gmail.readonly.
            reason.equals("insufficientPermissions", ignoreCase = true) ||
                reason.equals("insufficientScopes", ignoreCase = true) ->
                "Gmail says the sign-in didn't grant gmail.readonly " +
                    "(reason: $reason). Sign in again to grant it."
            reason.equals("accessNotConfigured", ignoreCase = true) ->
                "Gmail API is not enabled for this project " +
                    "(reason: accessNotConfigured). Enable the Gmail API in Google Cloud Console."
            reason.isNotEmpty() -> "$cleanMessage (reason: $reason)"
            // No reason AND no message: Gmail returned a 403 with no
            // explanation in the response. Say so (don't pretend we
            // know why), and name the two things worth checking.
            else -> "Gmail returned a 403 with no details. " +
                "This usually means the request was blocked or the " +
                "project's Gmail quota is exhausted. Try again in a bit."
        }
    }

    private fun buildQuery(params: Map<String, String>): String =
        params.entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }

    private fun enc(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    companion object {
        private const val TAG = "HttpGmailSyncApi"
        private const val MAX_PAGE_SIZE = 25
        /**
         * Bounded parallelism for per-message metadata fetches
         * (2026-10-17: lowered 8 -> 4). Gmail throttles per-user request
         * rate (userRateLimitExceeded); 8 parallel fetches plus repeated
         * manual "Try again" taps hit that throttle. 4 is still fast
         * (the 500-message first sync stays well under ~2 minutes) and
         * halves the peak request rate, so the import stops tripping
         * Gmail's per-second throttle.
         */
        private const val FETCH_CONCURRENCY = 4

        /**
         * Maps a Gmail `messages.get` metadata response to [RemoteMessage].
         * Pure function — unit-tested. Returns null when required fields
         * are missing.
         */
        fun parseMetadataMessage(
            json: JsonParser.JsonValue,
            fallbackId: String,
            fallbackThreadId: String,
        ): RemoteMessage? {
            val id = json.string("id") ?: fallbackId
            val threadId = json.string("threadId") ?: fallbackThreadId
            val labelIds = json.array("labelIds").orEmpty()
                .mapNotNull { (it as? JsonParser.JsonValue.Str)?.value }
            val snippet = json.string("snippet")
            val internalMs = json.string("internalDate")?.toLongOrNull()
                ?: json.long("internalDate")

            val headers = json.obj("payload")?.array("headers").orEmpty()
                .mapNotNull { h ->
                    val name = h.string("name")
                    val value = h.string("value")
                    if (name != null && value != null) name to value else null
                }.toMap()
            val headerMap = headers.mapKeys { it.key.lowercase() }

            val (fromName, fromAddress) = parseAddress(headerMap["from"].orEmpty())
            if (fromAddress.isBlank()) return null

            return RemoteMessage(
                id = id,
                threadId = threadId,
                fromAddress = fromAddress,
                fromName = fromName,
                toAddresses = parseAddressList(headerMap["to"].orEmpty()),
                ccAddresses = parseAddressList(headerMap["cc"].orEmpty()),
                subject = headerMap["subject"].orEmpty(),
                snippet = snippet,
                bodyText = null, // metadata format: bodies arrive on open
                timestampEpochMs = internalMs ?: System.currentTimeMillis(),
                unread = "UNREAD" in labelIds,
                starred = "STARRED" in labelIds,
                labels = labelIds,
                sizeBytes = json.string("sizeEstimate")?.toLongOrNull()
                    ?: json.long("sizeEstimate"),
            )
        }

        /**
         * Parses `Display Name <addr@x>` / `"Display Name" <addr@x>` /
         * bare `addr@x`. Pure function — unit-tested.
         */
        fun parseAddress(raw: String): Pair<String?, String> {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null to ""
            val angle = Regex("<([^<>]+)>").find(trimmed)
            if (angle != null) {
                val address = angle.groupValues[1].trim()
                val name = trimmed.substringBefore("<").trim()
                    .removeSurrounding("\"").trim().ifBlank { null }
                return name to address
            }
            // Bare address (maybe with a display name but no brackets —
            // take the last token if it looks like an email).
            val tokens = trimmed.split(Regex("\\s+"))
            val emailToken = tokens.lastOrNull { "@" in it } ?: trimmed
            val address = emailToken.trim().removeSurrounding("\"")
            val name = tokens.dropLastWhile { it == emailToken }
                .joinToString(" ").removeSurrounding("\"").trim().ifBlank { null }
            return name to address
        }

        private fun parseAddressList(raw: String): List<String> =
            raw.split(",")
                .map { parseAddress(it).second }
                .filter { it.isNotBlank() }
    }
}
