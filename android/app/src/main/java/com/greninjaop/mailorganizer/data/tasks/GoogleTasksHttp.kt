package com.greninjaop.mailorganizer.data.tasks

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.JsonParser
import com.greninjaop.mailorganizer.core.JsonParser.array
import com.greninjaop.mailorganizer.core.JsonParser.string
import com.greninjaop.mailorganizer.core.MoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.UrlConnectionTransport
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Minimal HTTPS client for the Google Tasks API v1 (Phase 16 §5).
 *
 * Uses [HttpURLConnection] directly — no Google client libraries — and
 * always talks to Google over HTTPS. The access token is passed per call
 * and never stored, persisted, or logged here; callers obtain it via
 * `AuthRepository.getValidAccessToken` and must treat it as sensitive.
 *
 * [baseUrl] is injectable so unit tests can run the full request/response
 * cycle against a local fake server. Production code always uses the
 * default `https://tasks.googleapis.com/tasks/v1`.
 *
 * Retry-safety (phase §27): creation is made idempotent by the caller
 * ([TasksRepository]) via [TaskDedupe] markers — this client performs
 * exactly one HTTP attempt per call and reports network failures
 * honestly instead of retrying blindly.
 */
class GoogleTasksHttp(
    private val baseUrl: String = "https://tasks.googleapis.com/tasks/v1",
    private val transport: HttpTransport = UrlConnectionTransport(),
) {

    // ---------- Task lists ----------

    /**
     * `GET /users/@me/lists` — the user's task lists (phase §11).
     */
    suspend fun listTaskLists(accessToken: String): MoResult<List<GoogleTaskList>> =
        withContext(Dispatchers.IO) {
            val result = get(accessToken, "$baseUrl/users/@me/lists")
            when (result) {
                is MoResult.Failure -> result
                is MoResult.Success -> {
                    val lists = parseTaskListsResponse(result.value)
                        ?: return@withContext MoResult.Failure(
                            MoError.Parsing("Couldn't understand the task-lists response."),
                        )
                    MoResult.Success(lists)
                }
            }
        }

    // ---------- Tasks ----------

    /**
     * `GET /lists/{listId}/tasks` — one page of tasks.
     * Used for display and for the idempotency pre-create scan.
     */
    suspend fun listTasks(
        accessToken: String,
        taskListId: String,
        pageToken: String? = null,
        maxResults: Int = MAX_PAGE_SIZE,
    ): MoResult<TaskPage> = withContext(Dispatchers.IO) {
        val query = buildString {
            append("?maxResults=").append(maxResults.coerceIn(1, MAX_PAGE_SIZE))
            append("&showDeleted=false&showCompleted=true")
            if (!pageToken.isNullOrBlank()) append("&pageToken=").append(urlEncodeQuery(pageToken))
        }
        val result = get(accessToken, "$baseUrl/lists/${encodePath(taskListId)}/tasks$query")
        when (result) {
            is MoResult.Failure -> result
            is MoResult.Success -> {
                val page = parseTasksResponse(result.value)
                    ?: return@withContext MoResult.Failure(
                        MoError.Parsing("Couldn't understand the tasks response."),
                    )
                MoResult.Success(page)
            }
        }
    }

    /**
     * `GET /lists/{listId}/tasks/{taskId}` — fetch one task.
     * Used to verify a creation result (phase §28–§29).
     */
    suspend fun getTask(
        accessToken: String,
        taskListId: String,
        taskId: String,
    ): MoResult<GoogleTask> = withContext(Dispatchers.IO) {
        val result = get(
            accessToken,
            "$baseUrl/lists/${encodePath(taskListId)}/tasks/${encodePath(taskId)}",
        )
        when (result) {
            is MoResult.Failure -> result
            is MoResult.Success -> {
                val task = parseTask(result.value)
                    ?: return@withContext MoResult.Failure(
                        MoError.Parsing("Couldn't understand the task response."),
                    )
                MoResult.Success(task.copy(taskListId = taskListId))
            }
        }
    }

    /**
     * `POST /lists/{listId}/tasks` — create a task (phase §24–§25).
     * Exactly one attempt; callers own idempotency via [TaskDedupe].
     */
    suspend fun insertTask(
        accessToken: String,
        taskListId: String,
        draft: TaskDraft,
    ): MoResult<GoogleTask> = withContext(Dispatchers.IO) {
        val body = buildInsertBody(draft)
        val result = post(
            accessToken,
            "$baseUrl/lists/${encodePath(taskListId)}/tasks",
            body,
        )
        when (result) {
            is MoResult.Failure -> result
            is MoResult.Success -> {
                val task = parseTask(result.value)
                    ?: return@withContext MoResult.Failure(
                        MoError.Parsing("Couldn't understand the created-task response."),
                    )
                if (task.id.isBlank()) {
                    return@withContext MoResult.Failure(
                        MoError.Parsing("The task response had no id."),
                    )
                }
                MoResult.Success(task.copy(taskListId = taskListId))
            }
        }
    }

    /**
     * `PATCH /lists/{listId}/tasks/{taskId}` — update a task
     * (phase §31 preparation: rename, reschedule, complete/reopen).
     *
     * Sent as POST with `X-HTTP-Method-Override: PATCH` because
     * [HttpURLConnection] rejects the PATCH method; Google's API
     * frontend honors the override header.
     */
    suspend fun patchTask(
        accessToken: String,
        taskListId: String,
        taskId: String,
        patch: TaskPatch,
    ): MoResult<GoogleTask> = withContext(Dispatchers.IO) {
        val body = buildPatchBody(patch)
        val result = request(
            accessToken,
            "POST",
            "$baseUrl/lists/${encodePath(taskListId)}/tasks/${encodePath(taskId)}",
            body,
            extraHeaders = mapOf("X-HTTP-Method-Override" to "PATCH"),
        )
        when (result) {
            is MoResult.Failure -> result
            is MoResult.Success -> {
                val task = parseTask(result.value)
                    ?: return@withContext MoResult.Failure(
                        MoError.Parsing("Couldn't understand the updated-task response."),
                    )
                MoResult.Success(task.copy(taskListId = taskListId))
            }
        }
    }

    /**
     * `DELETE /lists/{listId}/tasks/{taskId}` — delete a task
     * (phase §31 preparation).
     */
    suspend fun deleteTask(
        accessToken: String,
        taskListId: String,
        taskId: String,
    ): MoResult<Unit> = withContext(Dispatchers.IO) {
        val result = request(
            accessToken,
            "DELETE",
            "$baseUrl/lists/${encodePath(taskListId)}/tasks/${encodePath(taskId)}",
            body = null,
        )
        when (result) {
            is MoResult.Failure -> result
            is MoResult.Success -> MoResult.Success(Unit)
        }
    }

    // ---------- Transport ----------

    private fun get(accessToken: String, url: String): MoResult<String> =
        request(accessToken, "GET", url, body = null)

    private fun post(accessToken: String, url: String, body: String): MoResult<String> =
        request(accessToken, "POST", url, body)

    /**
     * One HTTP attempt. Never retries internally (phase §27, §34):
     * callers decide retry policy; offline is reported, not hammered.
     */
    private fun request(
        accessToken: String,
        method: String,
        url: String,
        body: String?,
        extraHeaders: Map<String, String> = emptyMap(),
    ): MoResult<String> {
        try {
            val headers = mutableMapOf(
                // The token travels only in this header; it is never logged.
                "Authorization" to "Bearer $accessToken",
                "Accept" to "application/json",
            )
            headers.putAll(extraHeaders)
            if (body != null) {
                headers["Content-Type"] = "application/json; charset=utf-8"
            }
            val response = transport.execute(method, url, headers, body)
            val code = response.code
            val responseBody = response.body.take(MAX_BODY_CHARS)
            if (code in 200..299) {
                return MoResult.Success(responseBody)
            }
            return MoResult.Failure(mapHttpError(code, responseBody))
        } catch (e: IOException) {
            MoLogger.w(TAG, "Tasks API call failed: ${e.javaClass.simpleName}")
            return MoResult.Failure(MoError.Network("Couldn't reach Google Tasks."))
        } catch (e: SecurityException) {
            MoLogger.w(TAG, "Tasks API call blocked: ${e.javaClass.simpleName}")
            return MoResult.Failure(MoError.Network("Couldn't reach Google Tasks."))
        }
    }

    // ---------- Pure helpers (unit-tested) ----------

    /**
     * Builds the `tasks.insert` JSON body. Pure function.
     */
    fun buildInsertBody(draft: TaskDraft): String {
        val notes = withDedupeMarker(draft.notes, draft.idempotencyKey)
        return buildString {
            append("{\"title\":\"").append(jsonEscape(draft.title)).append("\"")
            if (notes != null) {
                append(",\"notes\":\"").append(jsonEscape(notes)).append("\"")
            }
            if (draft.dueEpochMs != null) {
                append(",\"due\":\"").append(formatTasksDue(draft.dueEpochMs)).append("\"")
            }
            append("}")
        }
    }

    /**
     * Builds the `tasks.patch` JSON body. Pure function.
     */
    fun buildPatchBody(patch: TaskPatch): String = buildString {
        append("{")
        var first = true
        fun field(fragment: String) {
            if (!first) append(",")
            append(fragment)
            first = false
        }
        if (patch.title != null) field("\"title\":\"${jsonEscape(patch.title)}\"")
        if (patch.notes != null) field("\"notes\":\"${jsonEscape(patch.notes)}\"")
        if (patch.clearDue) {
            field("\"due\":null")
        } else if (patch.dueEpochMs != null) {
            field("\"due\":\"${formatTasksDue(patch.dueEpochMs)}\"")
        }
        if (patch.completed != null) {
            val status = if (patch.completed) "completed" else "needsAction"
            field("\"status\":\"$status\"")
        }
        append("}")
    }

    /** Parses a `tasklists.list` response body. Pure function. */
    fun parseTaskListsResponse(body: String): List<GoogleTaskList>? {
        val root = JsonParser.parse(body) ?: return null
        val items = root.array("items") ?: return emptyList()
        return items.mapNotNull { item ->
            val id = item.string("id")?.ifBlank { null } ?: return@mapNotNull null
            GoogleTaskList(
                id = id,
                title = item.string("title").orEmpty(),
                updatedEpochMs = item.string("updated")?.let { parseTasksDue(it) },
            )
        }
    }

    /** Parses a `tasks.list` response body. Pure function. */
    fun parseTasksResponse(body: String): TaskPage? {
        val root = JsonParser.parse(body) ?: return null
        val items = root.array("items")
        val tasks = items?.mapNotNull { item -> parseTaskObject(item) }.orEmpty()
        return TaskPage(
            tasks = tasks,
            nextPageToken = root.string("nextPageToken")?.ifBlank { null },
        )
    }

    /** Parses a single task resource body. Pure function. */
    fun parseTask(body: String): GoogleTask? {
        val root = JsonParser.parse(body) ?: return null
        return parseTaskObject(root)
    }

    private fun parseTaskObject(value: JsonParser.JsonValue): GoogleTask? {
        val id = value.string("id")?.ifBlank { null } ?: return null
        return GoogleTask(
            id = id,
            title = value.string("title").orEmpty(),
            notes = value.string("notes"),
            status = TaskStatus.fromApi(value.string("status")),
            dueEpochMs = value.string("due")?.let { parseTasksDue(it) },
            updatedEpochMs = value.string("updated")?.let { parseTasksDue(it) },
        )
    }

    /**
     * Maps an HTTP failure to a typed [MoError] (phase §35–§38).
     * Pure function — unit-tested.
     *
     * - 401 → [MoError.Authentication] (expired/invalid/revoked token;
     *   the auth layer's refresh already ran before this call).
     * - 403 → [MoError.PermissionDenied] when the body signals missing
     *   scope (incremental auth can fix it), else a generic API error.
     * - 404 → [MoError.Api] (unknown list/task — never retried blindly).
     * - 429 → [MoError.RateLimited] honoring Retry-After when present.
     * - 5xx → [MoError.Api] (transient; single retry policy lives above).
     */
    fun mapHttpError(code: Int, body: String, retryAfterSeconds: Long? = null): MoError {
        return when (code) {
            401 -> MoError.Authentication(
                "Google Tasks authorization expired or was revoked.",
            )
            403 -> {
                if (isInsufficientScope(body)) {
                    MoError.PermissionDenied(SCOPE_TASKS_URL)
                } else {
                    MoError.Api(403, "Google Tasks refused the request.")
                }
            }
            404 -> MoError.Api(404, "The task list or task wasn't found.")
            429 -> MoError.RateLimited(retryAfterSeconds)
            in 500..599 -> MoError.Api(code, "Google Tasks is having trouble right now.")
            400 -> MoError.Api(400, "The task request was invalid.")
            else -> MoError.Api(code, "Google Tasks request failed (HTTP $code).")
        }
    }

    private fun isInsufficientScope(body: String): Boolean {
        // Google's canonical insufficient-permissions reason.
        return body.contains("insufficient", ignoreCase = true) &&
            (body.contains("scope", ignoreCase = true) ||
                body.contains("permission", ignoreCase = true))
    }

    /**
     * Formats a due instant for the Tasks API `due` field (phase §21).
     *
     * The API's `due` is an RFC 3339 timestamp whose time portion is
     * not meaningfully surfaced — only the date matters. To avoid a
     * timezone shifting the *date* (e.g. IST midnight → previous day
     * in UTC), the device-timezone calendar date is emitted at UTC
     * midnight. An exact clock time, when the intelligence provides one,
     * is preserved in the task notes instead of being faked here.
     * Pure function — unit-tested.
     */
    fun formatTasksDue(epochMs: Long): String {
        val deviceCal = Calendar.getInstance(TimeZone.getDefault(), Locale.US).apply {
            timeInMillis = epochMs
        }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            clear()
            set(
                deviceCal.get(Calendar.YEAR),
                deviceCal.get(Calendar.MONTH),
                deviceCal.get(Calendar.DAY_OF_MONTH),
                0, 0, 0,
            )
            set(Calendar.MILLISECOND, 0)
        }
        return "%04d-%02d-%02dT00:00:00.000Z".format(
            Locale.US,
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH) + 1,
            utc.get(Calendar.DAY_OF_MONTH),
        )
    }

    /**
     * Parses an RFC 3339 timestamp from a Tasks response back to epoch
     * millis. Lenient: accepts `Z` and numeric offsets, with or without
     * fractional seconds. Null on unparseable input. Pure function.
     */
    fun parseTasksDue(value: String): Long? {
        val match = RFC3339.matchEntire(value.trim()) ?: return null
        return try {
            val year = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            val day = match.groupValues[3].toInt()
            val hour = match.groupValues[4].toInt()
            val minute = match.groupValues[5].toInt()
            val second = match.groupValues[6].toInt()
            val fraction = match.groupValues[7]
            val millis = when {
                fraction.isEmpty() -> 0
                else -> (fraction + "000").substring(0, 3).toInt()
            }
            val tz = match.groupValues[8]
            val zone: TimeZone = if (tz == "Z" || tz.isEmpty()) {
                TimeZone.getTimeZone("UTC")
            } else {
                TimeZone.getTimeZone("GMT$tz")
            }
            val cal = Calendar.getInstance(zone, Locale.US).apply {
                clear()
                set(year, month - 1, day, hour, minute, second)
                set(Calendar.MILLISECOND, millis)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Appends the idempotency marker to notes (phase §27). The marker is
     * compact, documented, and the only machine-readable line — the rest
     * of the notes stay human-friendly (phase §18).
     */
    fun withDedupeMarker(notes: String?, idempotencyKey: String): String? {
        val marker = TaskDedupe.marker(idempotencyKey)
        val trimmed = notes?.trim().orEmpty()
        return if (trimmed.isEmpty()) marker else "$trimmed\n$marker"
    }

    private fun urlEncodeQuery(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

    /**
     * Encodes one URL path segment. `@` is left intact so the
     * `@default` list alias keeps working.
     */
    fun encodePath(segment: String): String {
        val sb = StringBuilder()
        for (c in segment) {
            when {
                c.isLetterOrDigit() || c in "-._~!$&'()*+,;=:@" -> sb.append(c)
                else -> {
                    val bytes = c.toString().toByteArray(Charsets.UTF_8)
                    for (b in bytes) sb.append("%" + "%02X".format(b))
                }
            }
        }
        return sb.toString()
    }

    companion object {
        private const val TAG = "GoogleTasksHttp"
        private const val MAX_BODY_CHARS = 65_536
        private const val MAX_PAGE_SIZE = 100
        private const val SCOPE_TASKS_URL = "https://www.googleapis.com/auth/tasks"

        private val RFC3339 = Regex(
            """(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?(Z|[+-]\d{2}:?\d{2})?""",
        )
    }
}
