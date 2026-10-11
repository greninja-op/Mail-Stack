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

    override suspend fun fetchPage(
        accountId: AccountId,
        pageToken: String?,
        pageSize: Int,
    ): MessagePage = withContext(Dispatchers.IO) {
        val token = requireToken(accountId)
        val capped = pageSize.coerceIn(1, MAX_PAGE_SIZE)
        val params = mutableMapOf(
            "maxResults" to capped.toString(),
            // Sync the primary mailbox view. Label filtering for
            // Promotional/Social/Spam happens locally (Phase 7/8).
        )
        if (pageToken != null) params["pageToken"] = pageToken
        val listJson = get(token, "$apiBase/gmail/v1/users/me/messages", params)
            ?: throw SyncApiException.NetworkError()

        val messages = listJson.array("messages").orEmpty().mapNotNull { item ->
            val id = item.string("id")
            val threadId = item.string("threadId")
            if (id != null && threadId != null) id to threadId else null
        }
        val nextPageToken = listJson.string("nextPageToken")

        // Fetch metadata for each message (sequential; page sizes are small).
        val remote = messages.mapNotNull { (id, threadId) ->
            try {
                fetchMessageMetadata(token, id, threadId)
            } catch (e: SyncApiException) {
                // A single message failing must not kill the whole page;
                // 404s (deleted between list and get) are simply skipped.
                MoLogger.w(TAG, "Skipping message $id: ${e.javaClass.simpleName}")
                null
            }
        }
        MessagePage(messages = remote, nextPageToken = nextPageToken)
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
        val remote = addedIds.mapNotNull { (id, threadId) ->
            try {
                fetchMessageMetadata(token, id, threadId)
            } catch (e: SyncApiException) {
                MoLogger.w(TAG, "Skipping changed message $id: ${e.javaClass.simpleName}")
                null
            }
        }
        ChangePage(
            messages = remote,
            deletedRemoteIds = deletedIds.distinct(),
            nextHistoryCursor = json.string("historyId") ?: historyCursor,
        )
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
        val trimmed = body.take(500)
        val reason = JsonParser.parse(trimmed)?.obj("error")?.string("reason").orEmpty()
        val message = JsonParser.parse(trimmed)?.obj("error")?.string("message").orEmpty()
        MoLogger.w(TAG, "Gmail API HTTP $code reason=$reason url=${url.substringBefore("?")}")
        throw when {
            code == 401 -> SyncApiException.AuthExpired()
            code == 404 && url.contains("/history?") -> SyncApiException.HistoryInvalid()
            code == 404 -> SyncApiException.InvalidRequest("Not found: $message".take(120))
            code == 429 -> SyncApiException.RateLimited(null)
            code == 403 && reason.contains("RateLimitExceeded", ignoreCase = true) ->
                SyncApiException.RateLimited(null)
            code == 403 && reason.contains("UserRateLimitExceeded", ignoreCase = true) ->
                SyncApiException.RateLimited(null)
            code == 403 -> SyncApiException.PermissionDenied()
            code in 500..599 -> SyncApiException.ServerError(code)
            else -> SyncApiException.InvalidRequest("Gmail API HTTP $code".take(120))
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
