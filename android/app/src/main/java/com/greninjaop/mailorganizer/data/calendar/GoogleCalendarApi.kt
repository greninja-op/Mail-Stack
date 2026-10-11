package com.greninjaop.mailorganizer.data.calendar

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.UrlConnectionTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Google Calendar API client over HTTPS (Phase 15 §5, §13).
 *
 * Uses [HttpTransport] (default [UrlConnectionTransport]) — no heavy Google
 * client libraries — against the official `https://www.googleapis.com/calendar/v3` REST API.
 * The caller supplies a Bearer access token obtained ONLY via
 * `AuthRepository.getValidAccessToken(accountId)`; this client never
 * touches token storage and never logs tokens.
 *
 * [apiBase] is injectable so unit tests run the full flow against a local
 * fake server. Production code always uses the default Google endpoint.
 *
 * Idempotency strategy (Phase 15 §27–§29):
 * - Every insert carries a client-generated [EventWrite.iCalUID] plus a
 *   `mailstackKey` private extended property holding the deterministic
 *   idempotency key.
 * - Before inserting, [findEventByIdempotencyKey] recovers events whose
 *   insert response was lost (retry after network failure).
 * - The process-local receipt cache in the adapter is an optimization;
 *   the server-side extended-property lookup is the cross-process safety
 *   net. Retries with the same key can never create duplicates.
 */
class GoogleCalendarApi(
    private val apiBase: String = "https://www.googleapis.com/calendar/v3",
    private val transport: HttpTransport = UrlConnectionTransport(),
) {

    // ---------- Lightweight access probe ----------

    /**
     * Minimal validation that the token works AND the calendar scope is
     * granted (Phase 15 §38: never claim CONNECTED without proof).
     * Requests a single calendar-list entry with minimal fields.
     */
    suspend fun probeAccess(accessToken: String): CalendarApiResult<Unit> =
        withContext(Dispatchers.IO) {
            when (val r = get(
                accessToken,
                "$apiBase/users/me/calendarList?maxResults=1&fields=items(id)",
            )) {
                is CalendarApiResult.Success -> CalendarApiResult.Success(Unit)
                is CalendarApiResult.Failure -> CalendarApiResult.Failure(r.error)
            }
        }

    // ---------- Calendars ----------

    suspend fun listCalendars(accessToken: String): CalendarApiResult<List<CalendarInfo>> =
        withContext(Dispatchers.IO) {
            val url = "$apiBase/users/me/calendarList" +
                "?maxResults=100" +
                "&fields=${urlEncode("items(id,summary,description,primary,timeZone,accessRole)")}"
            when (val r = get(accessToken, url)) {
                is CalendarApiResult.Success -> {
                    val parsed = parseCalendarList(r.value)
                        ?: return@withContext CalendarApiResult.Failure(
                            CalendarApiError.MALFORMED_RESPONSE,
                        )
                    CalendarApiResult.Success(parsed)
                }
                is CalendarApiResult.Failure -> r
            }
        }

    // ---------- Events ----------

    suspend fun listUpcomingEvents(
        accessToken: String,
        calendarId: String,
        timeMinEpochMs: Long,
        maxResults: Int = 50,
    ): CalendarApiResult<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        val query = "timeMin=${urlEncode(formatRfc3339(timeMinEpochMs, "UTC"))}" +
            "&maxResults=${maxResults.coerceIn(1, 250)}" +
            "&singleEvents=true" +
            "&orderBy=startTime" +
            "&fields=${urlEncode("items(id,iCalUID,summary,status,start,end,location)")}"
        val url = "$apiBase/calendars/${encodePathSegment(calendarId)}/events?$query"
        when (val r = get(accessToken, url)) {
            is CalendarApiResult.Success -> {
                val parsed = parseEventList(r.value)
                    ?: return@withContext CalendarApiResult.Failure(
                        CalendarApiError.MALFORMED_RESPONSE,
                    )
                CalendarApiResult.Success(parsed)
            }
            is CalendarApiResult.Failure -> r
        }
    }

    /**
     * Finds an event previously created with [idempotencyKey] (stored as
     * the `mailstackKey` private extended property). Used for retry
     * recovery: if an insert's response was lost, this finds the event
     * instead of creating a duplicate.
     */
    suspend fun findEventByIdempotencyKey(
        accessToken: String,
        calendarId: String,
        idempotencyKey: String,
    ): CalendarApiResult<CalendarEvent?> = withContext(Dispatchers.IO) {
        val prop = urlEncode("$EXTENDED_KEY=$idempotencyKey")
        val url = "$apiBase/calendars/${encodePathSegment(calendarId)}/events" +
            "?privateExtendedProperty=$prop" +
            "&maxResults=1" +
            "&fields=${urlEncode("items(id,iCalUID,summary,status,start,end)")}"
        when (val r = get(accessToken, url)) {
            is CalendarApiResult.Success -> {
                val parsed = parseEventList(r.value)
                    ?: return@withContext CalendarApiResult.Failure(
                        CalendarApiError.MALFORMED_RESPONSE,
                    )
                CalendarApiResult.Success(parsed.firstOrNull())
            }
            is CalendarApiResult.Failure -> r
        }
    }

    suspend fun createEvent(
        accessToken: String,
        write: EventWrite,
    ): CalendarApiResult<CalendarEvent> = withContext(Dispatchers.IO) {
        val url = "$apiBase/calendars/${encodePathSegment(write.calendarId)}/events" +
            "?fields=${urlEncode("id,iCalUID,summary,status,start,end")}"
        val body = buildEventJson(write).toByteArray(Charsets.UTF_8)
        when (val r = post(accessToken, url, body)) {
            is CalendarApiResult.Success -> {
                val parsed = parseEvent(r.value)
                    ?: return@withContext CalendarApiResult.Failure(
                        CalendarApiError.MALFORMED_RESPONSE,
                    )
                CalendarApiResult.Success(parsed)
            }
            is CalendarApiResult.Failure -> r
        }
    }

    suspend fun getEvent(
        accessToken: String,
        calendarId: String,
        eventId: String,
    ): CalendarApiResult<CalendarEvent> = withContext(Dispatchers.IO) {
        val url = "$apiBase/calendars/${encodePathSegment(calendarId)}/events/" +
            "${encodePathSegment(eventId)}" +
            "?fields=${urlEncode("id,iCalUID,summary,status,start,end")}"
        when (val r = get(accessToken, url)) {
            is CalendarApiResult.Success -> {
                val parsed = parseEvent(r.value)
                    ?: return@withContext CalendarApiResult.Failure(
                        CalendarApiError.MALFORMED_RESPONSE,
                    )
                CalendarApiResult.Success(parsed)
            }
            is CalendarApiResult.Failure -> r
        }
    }

    suspend fun updateEvent(
        accessToken: String,
        write: EventWrite,
        eventId: String,
    ): CalendarApiResult<CalendarEvent> = withContext(Dispatchers.IO) {
        val url = "$apiBase/calendars/${encodePathSegment(write.calendarId)}/events/" +
            "${encodePathSegment(eventId)}" +
            "?fields=${urlEncode("id,iCalUID,summary,status,start,end")}"
        val body = buildEventJson(write).toByteArray(Charsets.UTF_8)
        when (val r = patch(accessToken, url, body)) {
            is CalendarApiResult.Success -> {
                val parsed = parseEvent(r.value)
                    ?: return@withContext CalendarApiResult.Failure(
                        CalendarApiError.MALFORMED_RESPONSE,
                    )
                CalendarApiResult.Success(parsed)
            }
            is CalendarApiResult.Failure -> r
        }
    }

    suspend fun deleteEvent(
        accessToken: String,
        calendarId: String,
        eventId: String,
    ): CalendarApiResult<Unit> = withContext(Dispatchers.IO) {
        val url = "$apiBase/calendars/${encodePathSegment(calendarId)}/events/" +
            encodePathSegment(eventId)
        delete(accessToken, url)
    }

    // ---------- HTTP primitives ----------

    private fun get(accessToken: String, url: String): CalendarApiResult<String> =
        request(accessToken, "GET", url, body = null, emptySuccessCodes = setOf(200))

    private fun post(
        accessToken: String,
        url: String,
        body: ByteArray,
    ): CalendarApiResult<String> =
        request(accessToken, "POST", url, body, emptySuccessCodes = setOf(200))

    private fun patch(
        accessToken: String,
        url: String,
        body: ByteArray,
    ): CalendarApiResult<String> =
        request(accessToken, "PATCH", url, body, emptySuccessCodes = setOf(200))

    private fun delete(accessToken: String, url: String): CalendarApiResult<Unit> {
        return when (val r = request(accessToken, "DELETE", url, null, setOf(204))) {
            is CalendarApiResult.Success -> CalendarApiResult.Success(Unit)
            is CalendarApiResult.Failure -> CalendarApiResult.Failure(r.error)
        }
    }

    private fun request(
        accessToken: String,
        method: String,
        url: String,
        body: ByteArray?,
        emptySuccessCodes: Set<Int>,
    ): CalendarApiResult<String> {
        try {
            val headers = mutableMapOf(
                // Never log the token; the Authorization header is set here
                // and never appears in any log statement in this file.
                "Authorization" to "Bearer $accessToken",
                "Accept" to "application/json",
            )
            if (body != null) {
                headers["Content-Type"] = "application/json; charset=utf-8"
            }
            val response = transport.execute(
                method = method,
                url = url,
                headers = headers,
                body = body?.toString(Charsets.UTF_8),
            )
            val code = response.code
            val responseBody = response.body.take(MAX_BODY_CHARS)
            if (code in emptySuccessCodes) {
                return CalendarApiResult.Success(responseBody)
            }
            val retryAfter = response.headers["retry-after"]?.toLongOrNull()
            return CalendarApiResult.Failure(
                mapHttpError(code, responseBody),
                retryAfterSeconds = retryAfter,
            )
        } catch (e: IOException) {
            MoLogger.w(TAG, "Calendar API call failed: ${e.javaClass.simpleName}")
            return CalendarApiResult.Failure(CalendarApiError.NETWORK)
        }
    }

    // ---------- Pure helpers (unit-tested) ----------

    /**
     * Builds the insert/patch request body. Pure function.
     *
     * Carries the client-generated [EventWrite.iCalUID] and the
     * `mailstackKey` private extended property so retries and recovery
     * lookups can identify the event deterministically.
     */
    fun buildEventJson(write: EventWrite): String {
        val startJson: String
        val endJson: String
        if (write.isAllDay) {
            val date = formatDate(write.startEpochMs, write.timeZoneId)
            startJson = "{\"date\":\"$date\"}"
            // All-day events need an end date; a single-day event ends the
            // next day (exclusive). Derived, not invented: same day span.
            val endDate = formatDate(write.startEpochMs + 86_400_000L, write.timeZoneId)
            endJson = "{\"date\":\"$endDate\"}"
        } else {
            val tz = write.timeZoneId
            startJson = "{\"dateTime\":\"${formatRfc3339(write.startEpochMs, tz)}\"," +
                "\"timeZone\":\"${CalendarJson.escape(tz)}\"}"
            val endMs = write.endEpochMs ?: write.startEpochMs
            endJson = "{\"dateTime\":\"${formatRfc3339(endMs, tz)}\"," +
                "\"timeZone\":\"${CalendarJson.escape(tz)}\"}"
        }
        return buildString {
            append("{")
            append("\"summary\":\"${CalendarJson.escape(write.summary)}\",")
            if (!write.description.isNullOrBlank()) {
                append("\"description\":\"${CalendarJson.escape(write.description)}\",")
            }
            if (!write.location.isNullOrBlank()) {
                append("\"location\":\"${CalendarJson.escape(write.location)}\",")
            }
            append("\"start\":$startJson,")
            append("\"end\":$endJson,")
            append("\"iCalUID\":\"${CalendarJson.escape(write.iCalUID)}\",")
            append("\"extendedProperties\":{\"private\":{" +
                "\"$EXTENDED_KEY\":\"${CalendarJson.escape(write.idempotencyKey)}\"}}")
            append("}")
        }
    }

    /** Parses a calendarList response. Null on malformed input. */
    fun parseCalendarList(body: String): List<CalendarInfo>? {
        val root = CalendarJson.parse(body) as? JsonValue.Obj ?: return null
        val items = root.arr("items") ?: return emptyList()
        return items.mapNotNull { item ->
            val id = item.str("id") ?: return@mapNotNull null
            CalendarInfo(
                id = id,
                summary = item.str("summary") ?: id,
                description = item.str("description"),
                primary = item.bool("primary") == true,
                timeZone = item.str("timeZone"),
                accessRole = item.str("accessRole") ?: "",
            )
        }
    }

    /** Parses an event resource. Null when the required id is missing. */
    fun parseEvent(body: String): CalendarEvent? {
        val root = CalendarJson.parse(body) as? JsonValue.Obj ?: return null
        val id = root.str("id") ?: return null
        return CalendarEvent(
            id = id,
            iCalUID = root.str("iCalUID"),
            summary = root.str("summary"),
            status = root.str("status"),
            start = parseEventTime(root.obj("start")),
            end = parseEventTime(root.obj("end")),
            location = root.str("location"),
        )
    }

    /** Parses an events.list response. Null on malformed input. */
    fun parseEventList(body: String): List<CalendarEvent>? {
        val root = CalendarJson.parse(body) as? JsonValue.Obj ?: return null
        val items = root.arr("items") ?: return emptyList()
        return items.mapNotNull { item ->
            val id = item.str("id") ?: return@mapNotNull null
            CalendarEvent(
                id = id,
                iCalUID = item.str("iCalUID"),
                summary = item.str("summary"),
                status = item.str("status"),
                start = parseEventTime(item.obj("start")),
                end = parseEventTime(item.obj("end")),
                location = item.str("location"),
            )
        }
    }

    private fun parseEventTime(obj: JsonValue.Obj?): EventTime? {
        if (obj == null) return null
        return EventTime(
            dateTime = obj.str("dateTime"),
            date = obj.str("date"),
            timeZone = obj.str("timeZone"),
        )
    }

    /**
     * Maps an HTTP failure to a typed error. Pure function.
     * Inspects the Google `error.errors[].reason` field — never surfaces
     * raw payloads to callers (they map to safe [MoError]s instead).
     */
    fun mapHttpError(code: Int, body: String): CalendarApiError {
        val reason = CalendarJson.parse(body)
            ?.obj("error")
            ?.arr("errors")
            ?.firstOrNull()
            ?.str("reason")
            .orEmpty()
        return when (code) {
            400 -> CalendarApiError.INVALID_REQUEST
            401 -> CalendarApiError.AUTH
            403 -> when (reason) {
                "rateLimitExceeded", "userRateLimitExceeded" ->
                    CalendarApiError.RATE_LIMITED
                else -> CalendarApiError.INSUFFICIENT_SCOPE
            }
            404 -> CalendarApiError.NOT_FOUND
            409, 412 -> CalendarApiError.CONFLICT
            429 -> CalendarApiError.RATE_LIMITED
            in 500..599 -> CalendarApiError.SERVER
            else -> CalendarApiError.SERVER
        }
    }

    /** RFC 3339 date-time with numeric offset, e.g. 2026-10-15T10:30:00+05:30. */
    fun formatRfc3339(epochMs: Long, timeZoneId: String): String {
        val zone = safeZoneId(timeZoneId)
        return java.time.OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    /** RFC 3339 full-date for all-day events, e.g. 2026-10-15. */
    fun formatDate(epochMs: Long, timeZoneId: String): String {
        val zone = safeZoneId(timeZoneId)
        return Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun safeZoneId(timeZoneId: String): ZoneId = runCatching {
        ZoneId.of(timeZoneId)
    }.getOrElse {
        MoLogger.w(TAG, "Unknown timezone id; falling back to system default")
        ZoneId.systemDefault()
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    /** Percent-encodes a single URL path segment. */
    fun encodePathSegment(segment: String): String = buildString {
        for (b in segment.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt().and(0xFF).toChar()
            if (c.isLetterOrDigit() || c == '-' || c == '_' || c == '.' || c == '~') {
                append(c)
            } else {
                append('%')
                append(HEX[(b.toInt() shr 4) and 0xF])
                append(HEX[b.toInt() and 0xF])
            }
        }
    }

    companion object {
        private const val TAG = "GoogleCalendarApi"
        private const val MAX_BODY_CHARS = 32_768

        /**
         * Private extended-property key carrying our deterministic
         * idempotency key on every event Mailstack creates. Server-side
         * lookup by this key is the retry-recovery mechanism (§29).
         */
        const val EXTENDED_KEY = "mailstackKey"

        private const val HEX = "0123456789ABCDEF"
    }
}

/** Typed result of a Calendar API call. Never carries tokens. */
sealed interface CalendarApiResult<out T> {
    data class Success<T>(val value: T) : CalendarApiResult<T>
    data class Failure(
        val error: CalendarApiError,
        /** Retry-After seconds when the server sent one (rate limiting). */
        val retryAfterSeconds: Long? = null,
    ) : CalendarApiResult<Nothing>

    /** Maps to the app-wide error taxonomy with safe, user-mappable messages. */
    fun toMoError(): MoError = when (this) {
        is Success -> throw IllegalStateException("toMoError on Success")
        is Failure -> error.toMoError(retryAfterSeconds)
    }
}

/** Safe Calendar API error categories (Phase 15 §31). */
enum class CalendarApiError {
    /** 401 — token expired/invalid/revoked. Caller must mark auth error. */
    AUTH,

    /** 403 — calendar scope missing or insufficient. Prompt incremental auth. */
    INSUFFICIENT_SCOPE,

    /** 403 with another reason. */
    FORBIDDEN,

    /** 404 — calendar or event not found. */
    NOT_FOUND,

    /** 409/412 — conflicting state (e.g. duplicate identity). */
    CONFLICT,

    /** 400 — malformed request (indicates a bug; never retried). */
    INVALID_REQUEST,

    /** 403 rateLimitExceeded / 429 — back off, honor Retry-After. */
    RATE_LIMITED,

    /** 5xx — Google-side failure. */
    SERVER,

    /** No connectivity / timeout. */
    NETWORK,

    /** Response could not be parsed. */
    MALFORMED_RESPONSE,
    ;

    fun toMoError(retryAfterSeconds: Long?): MoError = when (this) {
        AUTH -> MoError.Authentication(
            "Your Google sign-in expired or was revoked. Reconnect your account to continue.",
        )
        INSUFFICIENT_SCOPE -> MoError.PermissionDenied("calendar.events")
        FORBIDDEN -> MoError.Api(403, "Google Calendar refused the request.")
        NOT_FOUND -> MoError.Api(404, "The calendar or event wasn't found.")
        CONFLICT -> MoError.Api(409, "An event with the same identity already exists.")
        INVALID_REQUEST -> MoError.Api(400, "The calendar request was invalid.")
        RATE_LIMITED -> MoError.RateLimited(retryAfterSeconds)
        SERVER -> MoError.Api(null, "Google Calendar is having trouble right now.")
        NETWORK -> MoError.Network("Couldn't reach Google Calendar. Check your connection.")
        MALFORMED_RESPONSE -> MoError.Parsing("Google Calendar returned an unexpected response.")
    }
}

/** Calendar metadata (Phase 15 §12) — domain-level, no Google API types leak. */
data class CalendarInfo(
    val id: String,
    val summary: String,
    val description: String?,
    val primary: Boolean,
    val timeZone: String?,
    val accessRole: String,
) {
    /** Only owner/writer calendars may be event targets (Phase 15 §11). */
    val isWritable: Boolean
        get() = accessRole == "owner" || accessRole == "writer"
}

/** Calendar event resource (Phase 15 §13) — domain-level. */
data class CalendarEvent(
    val id: String,
    val iCalUID: String?,
    val summary: String?,
    val status: String?,
    val start: EventTime?,
    val end: EventTime?,
    val location: String?,
)

/** Event start/end representation: timed or all-day. */
data class EventTime(
    val dateTime: String?,
    val date: String?,
    val timeZone: String?,
) {
    val isAllDay: Boolean get() = date != null && dateTime == null
}

/**
 * Event write model: everything needed for an insert/patch, with the
 * client-generated [iCalUID] and [idempotencyKey] for duplicate safety.
 */
data class EventWrite(
    val calendarId: String,
    val summary: String,
    val description: String?,
    val location: String?,
    val startEpochMs: Long,
    /** Null for all-day events (single-day span derived from start). */
    val endEpochMs: Long?,
    val isAllDay: Boolean,
    val timeZoneId: String,
    val iCalUID: String,
    val idempotencyKey: String,
)
