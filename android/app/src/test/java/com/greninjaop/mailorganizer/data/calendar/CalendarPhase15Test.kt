package com.greninjaop.mailorganizer.data.calendar

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.TransportResponse
import java.io.IOException
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.actions.ActionCandidate
import com.greninjaop.mailorganizer.core.actions.ActionConfidence
import com.greninjaop.mailorganizer.core.actions.ActionSource
import com.greninjaop.mailorganizer.core.actions.ActionUrgency
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.data.auth.AuthErrorReason
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.AuthResult
import com.greninjaop.mailorganizer.data.auth.GoogleAuthState
import com.greninjaop.mailorganizer.data.integrations.RealCalendarIntegrationAdapter
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.SenderRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.domain.calendar.CalendarActionExecutor
import com.greninjaop.mailorganizer.domain.calendar.CalendarEventProposal
import com.greninjaop.mailorganizer.domain.calendar.CalendarProposalBuilder
import com.greninjaop.mailorganizer.domain.calendar.ProposalValidation
import com.greninjaop.mailorganizer.domain.calendar.ResolvedTemporal
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 15 unit tests: Calendar JSON codec, API request building,
 * response parsing, error mapping, idempotency-key stability, proposal
 * validation, account isolation, and the adapter/executor flows against
 * a local fake Calendar API.
 *
 * Everything here is pure JVM — no android.* imports, no Robolectric.
 */
class CalendarPhase15Test {

    // ---------- CalendarJson parser ----------

    @Test
    fun `parse handles nested objects and arrays`() {
        val v = CalendarJson.parse(
            """{"items":[{"id":"a","primary":true},{"id":"b"}],"n":3,"x":null}""",
        )
        assertNotNull(v)
        val items = v!!.arr("items")
        assertNotNull(items)
        assertEquals(2, items!!.size)
        assertEquals("a", items[0].str("id"))
        assertEquals(true, items[0].bool("primary"))
        assertEquals(3L, v.num("n"))
    }

    @Test
    fun `parse handles escapes and unicode`() {
        val v = CalendarJson.parse("""{"s":"a\"b\\c\né"}""")
        assertEquals("a\"b\\c\né", v!!.str("s"))
    }

    @Test
    fun `parse rejects malformed input`() {
        assertNull(CalendarJson.parse("not json"))
        assertNull(CalendarJson.parse("""{"a":}"""))
        assertNull(CalendarJson.parse("""{"a" "b"}"""))
        assertNull(CalendarJson.parse(""))
        assertNull(CalendarJson.parse("""{"a":"unterminated}"""))
        assertNull(CalendarJson.parse("""[1,2"""))
    }

    @Test
    fun `stringify round-trips`() {
        val original = """{"a":"x\"y","n":1,"b":true,"z":null,"arr":[1,"s"],"o":{"k":"v"}}"""
        val parsed = CalendarJson.parse(original)
        assertNotNull(parsed)
        val again = CalendarJson.parse(CalendarJson.stringify(parsed!!))
        assertNotNull(again)
        assertEquals("x\"y", again!!.str("a"))
        assertEquals("v", again.obj("o")!!.str("k"))
    }

    @Test
    fun `escape handles control characters`() {
        val escaped = CalendarJson.escape("a\"b\\c\nd\te")
        assertEquals("a\\\"b\\\\c\\nd\\te", escaped)
    }

    // ---------- Event request building ----------

    private fun timedWrite() = EventWrite(
        calendarId = "primary",
        summary = "Design \"review\"",
        description = "Added from Mailstack.\n\nJoin: https://meet.example/abc",
        location = "Room 5",
        startEpochMs = ZonedDateTime.of(2026, 10, 15, 10, 30, 0, 0, ZoneId.of("Asia/Kolkata"))
            .toInstant().toEpochMilli(),
        endEpochMs = ZonedDateTime.of(2026, 10, 15, 11, 30, 0, 0, ZoneId.of("Asia/Kolkata"))
            .toInstant().toEpochMilli(),
        isAllDay = false,
        timeZoneId = "Asia/Kolkata",
        iCalUID = "abc123@mailstack.local",
        idempotencyKey = "deadbeef",
    )

    @Test
    fun `buildEventJson carries idempotency material and escapes`() {
        val api = GoogleCalendarApi()
        val json = api.buildEventJson(timedWrite())
        val parsed = CalendarJson.parse(json)
        assertNotNull(parsed)
        assertEquals("Design \"review\"", parsed!!.str("summary"))
        assertEquals("Room 5", parsed.str("location"))
        assertEquals("abc123@mailstack.local", parsed.str("iCalUID"))
        assertEquals("deadbeef", parsed.obj("extendedProperties")!!.obj("private")!!.str("mailstackKey"))
        val start = parsed.obj("start")!!
        assertEquals("2026-10-15T10:30:00+05:30", start.str("dateTime"))
        assertEquals("Asia/Kolkata", start.str("timeZone"))
        val end = parsed.obj("end")!!
        assertEquals("2026-10-15T11:30:00+05:30", end.str("dateTime"))
    }

    @Test
    fun `buildEventJson renders all-day events as dates`() {
        val api = GoogleCalendarApi()
        val startMs = ZonedDateTime.of(2026, 10, 15, 0, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
            .toInstant().toEpochMilli()
        val json = api.buildEventJson(timedWrite().copy(isAllDay = true, startEpochMs = startMs, endEpochMs = null))
        val parsed = CalendarJson.parse(json)!!
        assertEquals("2026-10-15", parsed.obj("start")!!.str("date"))
        // Single-day all-day event ends the next day (exclusive).
        assertEquals("2026-10-16", parsed.obj("end")!!.str("date"))
        assertNull(parsed.obj("start")!!.str("dateTime"))
    }

    @Test
    fun `formatRfc3339 and formatDate respect timezones`() {
        val api = GoogleCalendarApi()
        assertEquals("1970-01-01T00:00:00Z", api.formatRfc3339(0L, "UTC"))
        val ms = ZonedDateTime.of(2026, 10, 15, 10, 30, 0, 0, ZoneId.of("Asia/Kolkata"))
            .toInstant().toEpochMilli()
        assertEquals("2026-10-15T10:30:00+05:30", api.formatRfc3339(ms, "Asia/Kolkata"))
        assertEquals("2026-10-15", api.formatDate(ms, "Asia/Kolkata"))
        // Unknown zone falls back without throwing.
        assertTrue(api.formatRfc3339(ms, "No/Such_Zone").isNotBlank())
    }

    @Test
    fun `encodePathSegment encodes reserved characters`() {
        val api = GoogleCalendarApi()
        assertEquals("primary", api.encodePathSegment("primary"))
        assertEquals("user%40example.com", api.encodePathSegment("user@example.com"))
    }

    // ---------- Response parsing ----------

    @Test
    fun `parseCalendarList reads calendars and writability`() {
        val api = GoogleCalendarApi()
        val list = api.parseCalendarList(
            """{"items":[
              {"id":"primary","summary":"Personal","primary":true,"timeZone":"Asia/Kolkata","accessRole":"owner"},
              {"id":"work","summary":"Work","accessRole":"reader"},
              {"id":"fam","summary":"Family","accessRole":"writer"}
            ]}""",
        )
        assertNotNull(list)
        assertEquals(3, list!!.size)
        assertTrue(list[0].isWritable)
        assertTrue(list[0].primary)
        assertEquals("Asia/Kolkata", list[0].timeZone)
        assertFalse(list[1].isWritable)
        assertTrue(list[2].isWritable)
    }

    @Test
    fun `parseCalendarList tolerates empty and malformed`() {
        val api = GoogleCalendarApi()
        assertEquals(0, api.parseCalendarList("""{"items":[]}""")!!.size)
        assertNull(api.parseCalendarList("garbage"))
        assertNull(api.parseCalendarList("""{"items":[{"noid":1}]}""")?.let {
            if (it.isEmpty()) null else it
        })
    }

    @Test
    fun `parseEvent reads event resource`() {
        val api = GoogleCalendarApi()
        val event = api.parseEvent(
            """{"id":"evt1","iCalUID":"uid1","summary":"Standup","status":"confirmed",
               "start":{"dateTime":"2026-10-15T10:00:00+05:30","timeZone":"Asia/Kolkata"},
               "end":{"dateTime":"2026-10-15T10:30:00+05:30","timeZone":"Asia/Kolkata"},
               "location":"Room 1"}""",
        )
        assertNotNull(event)
        assertEquals("evt1", event!!.id)
        assertEquals("uid1", event.iCalUID)
        assertEquals("confirmed", event.status)
        assertEquals("2026-10-15T10:00:00+05:30", event.start!!.dateTime)
        assertFalse(event.start.isAllDay)
        assertEquals("Room 1", event.location)
    }

    @Test
    fun `parseEvent requires id`() {
        val api = GoogleCalendarApi()
        assertNull(api.parseEvent("""{"summary":"no id"}"""))
        assertNull(api.parseEvent("not json"))
    }

    @Test
    fun `parseEventList reads items`() {
        val api = GoogleCalendarApi()
        val list = api.parseEventList(
            """{"items":[{"id":"e1","summary":"A"},{"id":"e2","summary":"B"}]}""",
        )
        assertNotNull(list)
        assertEquals(listOf("e1", "e2"), list!!.map { it.id })
        assertEquals(0, api.parseEventList("""{"items":[]}""")!!.size)
    }

    // ---------- Error mapping ----------

    @Test
    fun `mapHttpError categorizes failures`() {
        val api = GoogleCalendarApi()
        assertEquals(CalendarApiError.AUTH, api.mapHttpError(401, "{}"))
        assertEquals(
            CalendarApiError.INSUFFICIENT_SCOPE,
            api.mapHttpError(
                403,
                """{"error":{"code":403,"errors":[{"reason":"insufficientPermissions"}]}}""",
            ),
        )
        assertEquals(
            CalendarApiError.RATE_LIMITED,
            api.mapHttpError(
                403,
                """{"error":{"code":403,"errors":[{"reason":"rateLimitExceeded"}]}}""",
            ),
        )
        assertEquals(CalendarApiError.NOT_FOUND, api.mapHttpError(404, "{}"))
        assertEquals(CalendarApiError.CONFLICT, api.mapHttpError(409, "{}"))
        assertEquals(CalendarApiError.CONFLICT, api.mapHttpError(412, "{}"))
        assertEquals(CalendarApiError.RATE_LIMITED, api.mapHttpError(429, "{}"))
        assertEquals(CalendarApiError.SERVER, api.mapHttpError(500, "{}"))
        assertEquals(CalendarApiError.INVALID_REQUEST, api.mapHttpError(400, "{}"))
    }

    @Test
    fun `CalendarApiError maps to safe MoErrors`() {
        assertTrue(CalendarApiError.AUTH.toMoError(null) is MoError.Authentication)
        val scope = CalendarApiError.INSUFFICIENT_SCOPE.toMoError(null)
        assertTrue(scope is MoError.PermissionDenied)
        assertEquals("calendar.events", (scope as MoError.PermissionDenied).permission)
        val rate = CalendarApiError.RATE_LIMITED.toMoError(120L)
        assertEquals(120L, (rate as MoError.RateLimited).retryAfterSeconds)
        assertTrue(CalendarApiError.NETWORK.toMoError(null) is MoError.Network)
        assertTrue(CalendarApiError.NOT_FOUND.toMoError(null) is MoError.Api)
    }

    // ---------- Idempotency keys & proposals ----------

    @Test
    fun `idempotencyKeyFor is deterministic and account-isolated`() {
        val a1 = CalendarProposalBuilder.idempotencyKeyFor("google_a", "primary", "act1")
        val a2 = CalendarProposalBuilder.idempotencyKeyFor("google_a", "primary", "act1")
        assertEquals(a1, a2)
        assertEquals(32, a1.length)
        assertTrue(a1.matches(Regex("[0-9a-f]+")))
        // Different account -> different key (never mix accounts).
        assertNotEquals(a1, CalendarProposalBuilder.idempotencyKeyFor("google_b", "primary", "act1"))
        // Different action -> different key.
        assertNotEquals(a1, CalendarProposalBuilder.idempotencyKeyFor("google_a", "primary", "act2"))
        // Different calendar -> different key.
        assertNotEquals(a1, CalendarProposalBuilder.idempotencyKeyFor("google_a", "work", "act1"))
    }

    private fun meetingCandidate(
        accountId: String = "google_abc",
        targetKey: String = "temporal:7",
        dueStart: Long? = 1_791_234_567_000L,
        title: String = "Design review",
    ) = ActionCandidate(
        id = "m1|MEETING|$targetKey",
        accountId = accountId,
        messageId = "m1",
        threadId = "t1",
        actionType = ActionType.MEETING,
        title = title,
        description = "Suggested meeting.",
        urgency = ActionUrgency.NORMAL,
        source = ActionSource.TEMPORAL_EXTRACTION,
        confidence = ActionConfidence.HIGH,
        explanation = "Suggested because ...",
        signals = emptyList(),
        externalEffect = ExternalEffect.CALENDAR,
        missingInfo = emptyList(),
        dueDateEpochMs = dueStart,
        targetKey = targetKey,
        version = 1,
    )

    private fun temporal(
        start: Long = 1_791_234_567_000L,
        end: Long? = 1_791_238_167_000L,
        dateOnly: Boolean = false,
    ) = ResolvedTemporal(
        title = "Design review",
        startEpochMs = start,
        endEpochMs = end,
        isDateOnly = dateOnly,
        timeZoneId = "Asia/Kolkata",
        location = "Room 5",
        meetingUrl = "https://meet.example/abc",
    )

    @Test
    fun `titleFor prefers explicit title then candidate then generic`() {
        val c = meetingCandidate(title = "Candidate title")
        assertEquals("Explicit", CalendarProposalBuilder.titleFor(c, temporal().copy(title = "Explicit")))
        assertEquals("Candidate title", CalendarProposalBuilder.titleFor(c, temporal().copy(title = "  ")))
        assertEquals(
            "Calendar event",
            CalendarProposalBuilder.titleFor(c.copy(title = " "), ResolvedTemporal(
                title = " ", startEpochMs = 1L, endEpochMs = null, isDateOnly = true,
                timeZoneId = "UTC", location = null, meetingUrl = null,
            )),
        )
    }

    @Test
    fun `descriptionFor is minimal and carries the join link`() {
        val c = meetingCandidate()
        val d = CalendarProposalBuilder.descriptionFor(c, temporal())
        assertTrue(d.contains("Added from Mailstack."))
        assertTrue(d.contains("https://meet.example/abc"))
        // No internal ids leak into the description.
        assertFalse(d.contains("m1|MEETING"))
        val d2 = CalendarProposalBuilder.descriptionFor(c, temporal().copy(meetingUrl = null))
        assertFalse(d2.contains("Join:"))
    }

    @Test
    fun `build returns null without a start time`() {
        assertNull(CalendarProposalBuilder.build(meetingCandidate(dueStart = null), null, "primary"))
        assertNull(CalendarProposalBuilder.build(meetingCandidate(dueStart = 0L), null, "primary"))
    }

    @Test
    fun `build drops end before start and stamps stable identity`() {
        val p = CalendarProposalBuilder.build(
            meetingCandidate(), temporal(end = 1L), "primary",
        )!!
        // End before start is dropped, not used.
        assertNull(p.endEpochMs)
        assertEquals("primary", p.calendarId)
        assertEquals(p.idempotencyKey, CalendarProposalBuilder.idempotencyKeyFor(
            "google_abc", "primary", "m1|MEETING|temporal:7",
        ))
        assertEquals("${p.idempotencyKey}@mailstack.local", p.iCalUID)
    }

    @Test
    fun `validate enforces the no-invented-end-time policy`() {
        val base = CalendarEventProposal(
            accountId = "google_abc", calendarId = "primary", title = "T",
            description = "d", startEpochMs = 1000L, endEpochMs = 2000L,
            isAllDay = false, timeZoneId = "UTC", location = null, meetingUrl = null,
            sourceMessageId = "m1", sourceActionId = "a1",
            idempotencyKey = "k", iCalUID = "k@mailstack.local",
        )
        assertEquals(ProposalValidation.Valid, CalendarProposalBuilder.validate(base))
        // Timed start without end: never invent a duration.
        assertTrue(
            CalendarProposalBuilder.validate(base.copy(endEpochMs = null))
                is ProposalValidation.Invalid,
        )
        // All-day without end is fine.
        assertEquals(
            ProposalValidation.Valid,
            CalendarProposalBuilder.validate(base.copy(endEpochMs = null, isAllDay = true)),
        )
        assertTrue(
            CalendarProposalBuilder.validate(base.copy(endEpochMs = 500L))
                is ProposalValidation.Invalid,
        )
        assertTrue(
            CalendarProposalBuilder.validate(base.copy(title = "  "))
                is ProposalValidation.Invalid,
        )
        assertTrue(
            CalendarProposalBuilder.validate(base.copy(calendarId = ""))
                is ProposalValidation.Invalid,
        )
    }

    @Test
    fun `previewText shows what confirmation needs`() {
        val p = CalendarProposalBuilder.build(meetingCandidate(), temporal(), "primary")!!
        val preview = CalendarProposalBuilder.previewText(p, "Personal")
        assertTrue(preview.contains("Add to Calendar?"))
        assertTrue(preview.contains("Design review"))
        assertTrue(preview.contains("Calendar: Personal"))
        assertTrue(preview.contains("Room 5"))
        assertTrue(preview.contains("https://meet.example/abc"))
        val allDay = CalendarProposalBuilder.build(
            meetingCandidate(), temporal(dateOnly = true, end = null), "primary",
        )!!
        assertTrue(CalendarProposalBuilder.previewText(allDay, "Personal").contains("All-day"))
    }

    // ---------- Fake-server API tests ----------

    private fun fakeCalendarTransport(
        handler: (method: String, path: String, query: String?, body: String) -> Pair<Int, String>,
    ): HttpTransport = HttpTransport { method, url, _, body ->
        val afterHost = url.substringAfter("://").substringAfter("/")
        val path = "/" + afterHost.substringBefore("?")
        val query = afterHost.substringAfter("?", "").ifEmpty { null }
        val (code, response) = handler(method, path, query, body ?: "")
        TransportResponse(code, response)
    }

    private fun apiFor(transport: HttpTransport) =
        GoogleCalendarApi("http://fake.calendar/calendar/v3", transport)

    @Test
    fun `probeAccess succeeds on 200`() = runBlocking {
        val transport = fakeCalendarTransport { _, path, _, _ ->
            assertTrue(path.endsWith("/users/me/calendarList"))
            200 to """{"items":[{"id":"primary"}]}"""
        }
            val r = apiFor(transport).probeAccess("tok")
            assertTrue(r is CalendarApiResult.Success)
    }

    @Test
    fun `probeAccess maps 403 to INSUFFICIENT_SCOPE`() = runBlocking {
        val transport = fakeCalendarTransport { _, _, _, _ ->
            403 to """{"error":{"code":403,"errors":[{"reason":"insufficientPermissions"}]}}"""
        }
            val r = apiFor(transport).probeAccess("tok")
            val f = r as CalendarApiResult.Failure
            assertEquals(CalendarApiError.INSUFFICIENT_SCOPE, f.error)
    }

    @Test
    fun `probeAccess maps connection failure to NETWORK`() = runBlocking {
        // Transport throws -> NETWORK.
        val api = GoogleCalendarApi(
            "http://fake.calendar/calendar/v3",
            HttpTransport { _, _, _, _ -> throw IOException("simulated network failure") },
        )
        val r = api.probeAccess("tok")
        assertEquals(CalendarApiError.NETWORK, (r as CalendarApiResult.Failure).error)
    }

    @Test
    fun `createEvent posts idempotency material and parses the response`() = runBlocking {
        val posts = mutableListOf<String>()
        val transport = fakeCalendarTransport { method, path, _, body ->
            when {
                method == "POST" && path.endsWith("/events") -> {
                    posts += body
                    200 to """{"id":"evt-1","iCalUID":"k@mailstack.local","summary":"Design review","status":"confirmed"}"""
                }
                else -> 404 to "{}"
            }
        }
            val api = apiFor(transport)
            val r = api.createEvent("tok", timedWrite().copy(calendarId = "primary"))
            val created = (r as CalendarApiResult.Success).value
            assertEquals("evt-1", created.id)
            assertEquals(1, posts.size)
            val sent = CalendarJson.parse(posts[0])!!
            assertEquals("abc123@mailstack.local", sent.str("iCalUID"))
            assertEquals("deadbeef", sent.obj("extendedProperties")!!.obj("private")!!.str("mailstackKey"))
            assertEquals("Design \"review\"", sent.str("summary"))
    }

    @Test
    fun `findEventByIdempotencyKey encodes the lookup query`() = runBlocking {
        val queries = mutableListOf<String?>()
        val transport = fakeCalendarTransport { method, path, query, _ ->
            queries += query
            if (method == "GET" && path.endsWith("/events")) {
                200 to """{"items":[{"id":"evt-9","summary":"Existing"}]}"""
            } else {
                404 to "{}"
            }
        }
            val r = apiFor(transport).findEventByIdempotencyKey("tok", "primary", "my key/id")
            val found = (r as CalendarApiResult.Success).value
            assertEquals("evt-9", found!!.id)
            val q = queries.firstOrNull().orEmpty()
            assertTrue(q.contains("privateExtendedProperty="))
            // The key survives URL-decoding to the original value.
            assertTrue(java.net.URLDecoder.decode(q, "UTF-8").contains("my key/id"))
    }

    @Test
    fun `deleteEvent treats 204 as success`() = runBlocking {
        val transport = fakeCalendarTransport { method, _, _, _ ->
            if (method == "DELETE") 204 to "" else 404 to "{}"
        }
            val r = apiFor(transport).deleteEvent("tok", "primary", "evt-1")
            assertTrue(r is CalendarApiResult.Success)
    }

    // ---------- Adapter tests ----------

    private class StubAuth(
        var connected: Boolean = true,
        var token: String? = "test-token",
    ) : AuthRepository {
        override fun observeAuthStates(): Flow<Map<AccountId, GoogleAuthState>> =
            flowOf(emptyMap<AccountId, GoogleAuthState>())

        override fun observeAuthState(accountId: AccountId): Flow<GoogleAuthState> =
            flowOf(
                GoogleAuthState.Connected(accountId, "user@example.com", "User"),
            )

        override suspend fun getValidAccessToken(accountId: AccountId): String? = token

        override suspend fun handleOAuthResult(data: android.content.Intent?): AuthResult =
            AuthResult.Failure(AuthErrorReason.UNKNOWN)

        override fun createOAuthIntent(
            additionalScopes: List<String>,
        ): android.content.Intent = throw UnsupportedOperationException()

        override fun createIncrementalAuthIntent(
            accountId: AccountId,
            additionalScopes: List<String>,
        ): android.content.Intent = throw UnsupportedOperationException()

        override fun observeOAuthResults(): Flow<AuthResult> = flowOf()

        override suspend fun disconnect(accountId: AccountId) = Unit

        override suspend fun isConnected(accountId: AccountId): Boolean = connected
    }

    private fun calendarListJson() =
        """{"items":[{"id":"primary","summary":"Personal","primary":true,"timeZone":"Asia/Kolkata","accessRole":"owner"}]}"""

    @Test
    fun `snapshot is CONNECTED only after a successful probe`() = runBlocking {
        val transport = fakeCalendarTransport { _, path, _, _ ->
            if (path.endsWith("/users/me/calendarList")) 200 to """{"items":[{"id":"primary"}]}"""
            else 404 to "{}"
        }
            val adapter = RealCalendarIntegrationAdapter(StubAuth(), apiFor(transport))
            val snap = adapter.snapshot("google_abc")
            assertEquals(
                com.greninjaop.mailorganizer.core.integrations.IntegrationStatus.CONNECTED,
                snap.status,
            )
            // Usable capabilities are only exposed when CONNECTED.
            assertTrue(snap.usableCapabilities.isNotEmpty())
    }

    @Test
    fun `snapshot is PERMISSION_REQUIRED on 403`() = runBlocking {
        val transport = fakeCalendarTransport { _, _, _, _ ->
            403 to """{"error":{"code":403,"errors":[{"reason":"insufficientPermissions"}]}}"""
        }
            val adapter = RealCalendarIntegrationAdapter(StubAuth(), apiFor(transport))
            val snap = adapter.snapshot("google_abc")
            assertEquals(
                com.greninjaop.mailorganizer.core.integrations.IntegrationStatus.PERMISSION_REQUIRED,
                snap.status,
            )
            assertTrue(snap.usableCapabilities.isEmpty())
    }

    @Test
    fun `snapshot is OFFLINE on network failure, never DISCONNECTED`() = runBlocking {
        val adapter = RealCalendarIntegrationAdapter(
            StubAuth(),
            GoogleCalendarApi(
                "http://fake.calendar/calendar/v3",
                HttpTransport { _, _, _, _ -> throw IOException("simulated network failure") },
            ),
        )
        val snap = adapter.snapshot("google_abc")
        // Offline is not a disconnect (Phase 15 §32).
        assertEquals(
            com.greninjaop.mailorganizer.core.integrations.IntegrationStatus.OFFLINE,
            snap.status,
        )
    }

    @Test
    fun `snapshot is DISCONNECTED when the account is not connected`() = runBlocking {
        val adapter = RealCalendarIntegrationAdapter(StubAuth(connected = false), GoogleCalendarApi())
        val snap = adapter.snapshot("google_abc")
        assertEquals(
            com.greninjaop.mailorganizer.core.integrations.IntegrationStatus.DISCONNECTED,
            snap.status,
        )
    }

    @Test
    fun `snapshot is AUTH_REQUIRED when no token is available`() = runBlocking {
        val adapter = RealCalendarIntegrationAdapter(StubAuth(token = null), GoogleCalendarApi())
        val snap = adapter.snapshot("google_abc")
        assertEquals(
            com.greninjaop.mailorganizer.core.integrations.IntegrationStatus.AUTH_REQUIRED,
            snap.status,
        )
    }

    @Test
    fun `adapter createEvent is idempotent across retries`() = runBlocking {
        var posts = 0
        val transport = fakeCalendarTransport { method, path, query, body ->
            when {
                method == "GET" && path.endsWith("/events") && query?.contains("privateExtendedProperty=") == true ->
                    200 to """{"items":[]}"""
                method == "POST" && path.endsWith("/events") -> {
                    posts++
                    200 to """{"id":"evt-1","summary":"Design review","status":"confirmed"}"""
                }
                method == "GET" && path.endsWith("/events/evt-1") ->
                    200 to """{"id":"evt-1","summary":"Design review","status":"confirmed"}"""
                else -> 404 to "{}"
            }
        }
            val adapter = RealCalendarIntegrationAdapter(StubAuth(), apiFor(transport))
            val proposal = CalendarProposalBuilder.build(meetingCandidate(), temporal(), "primary")!!
            val first = adapter.createEvent(AccountId("google_abc"), proposal)
            assertEquals("evt-1", (first as MoResult.Success).value.id)
            // Retry with the same proposal: receipt cache + verification, no second POST.
            val second = adapter.createEvent(AccountId("google_abc"), proposal)
            assertEquals("evt-1", (second as MoResult.Success).value.id)
            assertEquals(1, posts)
    }

    @Test
    fun `adapter createEvent recovers an existing event after a cold retry`() = runBlocking {
        var posts = 0
        val transport = fakeCalendarTransport { method, path, query, _ ->
            when {
                method == "GET" && path.endsWith("/events") && query?.contains("privateExtendedProperty=") == true ->
                    200 to """{"items":[{"id":"evt-old","summary":"Design review","status":"confirmed"}]}"""
                method == "POST" && path.endsWith("/events") -> {
                    posts++
                    200 to """{"id":"evt-new","summary":"Design review","status":"confirmed"}"""
                }
                else -> 404 to "{}"
            }
        }
            // Fresh adapter: empty process-local cache (simulates a restart).
            val adapter = RealCalendarIntegrationAdapter(StubAuth(), apiFor(transport))
            val proposal = CalendarProposalBuilder.build(meetingCandidate(), temporal(), "primary")!!
            val r = adapter.createEvent(AccountId("google_abc"), proposal)
            // Server-side lookup found the earlier event: no duplicate insert.
            assertEquals("evt-old", (r as MoResult.Success).value.id)
            assertEquals(0, posts)
    }

    @Test
    fun `adapter createEvent rejects account mismatch`() = runBlocking {
        val adapter = RealCalendarIntegrationAdapter(StubAuth(), GoogleCalendarApi())
        val proposal = CalendarProposalBuilder.build(meetingCandidate(), temporal(), "primary")!!
        val r = adapter.createEvent(AccountId("google_OTHER"), proposal)
        assertTrue(r is MoResult.Failure)
        assertTrue((r as MoResult.Failure).error is MoError.Integration)
    }

    @Test
    fun `adapter createEvent maps 401 to Authentication`() = runBlocking {
        val transport = fakeCalendarTransport { method, path, query, _ ->
            if (method == "GET" && path.endsWith("/events") && query?.contains("privateExtendedProperty=") == true)
                200 to """{"items":[]}"""
            else 401 to """{"error":{"code":401}}"""
        }
            val adapter = RealCalendarIntegrationAdapter(StubAuth(), apiFor(transport))
            val proposal = CalendarProposalBuilder.build(meetingCandidate(), temporal(), "primary")!!
            val r = adapter.createEvent(AccountId("google_abc"), proposal)
            assertTrue((r as MoResult.Failure).error is MoError.Authentication)
    }

    // ---------- Executor tests ----------

    private class FakeIntelligence(
        private val extracted: List<ExtractedItemRecord> = emptyList(),
    ) : IntelligenceRepository {
        override suspend fun getExtractedItems(messageId: String): List<ExtractedItemRecord> =
            extracted.filter { it.messageId == messageId }

        override suspend fun upsertSender(sender: SenderRecord) = TODO()
        override fun observeTopSenders(accountId: String, limit: Int): Flow<List<SenderRecord>> = TODO()
        override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String): SenderRecord? = TODO()
        override suspend fun recordSenderMessage(
            accountId: String, emailAddress: String, normalizedEmail: String,
            displayName: String?, domain: String,
        ): SenderRecord = TODO()
        override suspend fun upsertCompany(company: CompanyRecord) = TODO()
        override fun observeCompanyFilterList(accountId: String, limit: Int): Flow<List<CompanyRecord>> = TODO()
        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) = TODO()
        override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String): CompanyRecord? = TODO()
        override suspend fun setClassification(record: ClassificationRecord) = TODO()
        override suspend fun getClassification(messageId: String): ClassificationRecord? = TODO()
        override suspend fun deleteClassification(messageId: String) = TODO()
        override suspend fun getClassifications(messageIds: List<String>): Map<String, ClassificationRecord> = TODO()
        override fun observeByCategory(
            accountId: String, category: MailCategory, limit: Int,
        ): Flow<List<ClassificationRecord>> = TODO()
        override suspend fun categoryCounts(accountId: String): Map<MailCategory, Int> = TODO()
        override suspend fun setPriority(record: PriorityRecord) = TODO()
        override suspend fun getPriority(messageId: String): PriorityRecord? = TODO()
        override suspend fun deletePriority(messageId: String) = TODO()
        override suspend fun getPriorities(messageIds: List<String>): Map<String, PriorityRecord> = TODO()
        override fun observeByPriority(
            accountId: String, priority: Priority, limit: Int,
        ): Flow<List<PriorityRecord>> = TODO()
        override suspend fun addActionItem(item: ActionItemRecord): Long = TODO()
        override fun observeOpenActionItems(accountId: String, limit: Int): Flow<List<ActionItemRecord>> = TODO()
        override suspend fun completeActionItem(id: Long) = TODO()
        override suspend fun dismissActionItem(id: Long) = TODO()
        override suspend fun getActionItem(id: Long): ActionItemRecord? = TODO()
        override suspend fun getActionItemsByMessage(messageId: String): List<ActionItemRecord> = TODO()
        override suspend fun getOpenActionItemsByThread(threadId: String): List<ActionItemRecord> = TODO()
        override suspend fun setActionItemStatus(id: Long, status: ActionStatus) = TODO()
        override suspend fun deleteActionItems(ids: List<Long>) = TODO()
        override suspend fun expireOverdueActionItems(accountId: String, cutoffEpochMs: Long): Int = TODO()
        override suspend fun addExtractedItem(item: ExtractedItemRecord): Long = TODO()
        override fun observeOpenExtracted(
            accountId: String, type: ExtractedItemType, limit: Int,
        ): Flow<List<ExtractedItemRecord>> = TODO()
        override suspend fun deleteExtractedItems(ids: List<Long>) = TODO()
    }

    @Test
    fun `executor supports only MEETING`() {
        val executor = CalendarActionExecutor(
            RealCalendarIntegrationAdapter(StubAuth(), GoogleCalendarApi()),
            FakeIntelligence(),
        )
        assertTrue(executor.supports(ActionType.MEETING))
        assertFalse(executor.supports(ActionType.REMINDER))
        assertFalse(executor.supports(ActionType.DEADLINE))
    }

    @Test
    fun `executor validate rejects malformed candidates`() {
        val executor = CalendarActionExecutor(
            RealCalendarIntegrationAdapter(StubAuth(), GoogleCalendarApi()),
            FakeIntelligence(),
        )
        val good = meetingCandidate()
        assertTrue(executor.validate(good) is MoResult.Success)
        assertTrue(
            executor.validate(good.copy(actionType = ActionType.REMINDER)) is MoResult.Failure,
        )
        assertTrue(
            executor.validate(good.copy(externalEffect = ExternalEffect.NONE)) is MoResult.Failure,
        )
        assertTrue(
            executor.validate(good.copy(accountId = "")) is MoResult.Failure,
        )
        assertTrue(
            executor.validate(good.copy(dueDateEpochMs = null)) is MoResult.Failure,
        )
    }

    private fun extractedRecord(): ExtractedItemRecord {
        val start = ZonedDateTime.of(2026, 10, 15, 10, 30, 0, 0, ZoneId.of("Asia/Kolkata"))
            .toInstant().toEpochMilli()
        val end = ZonedDateTime.of(2026, 10, 15, 11, 30, 0, 0, ZoneId.of("Asia/Kolkata"))
            .toInstant().toEpochMilli()
        return ExtractedItemRecord(
            id = 7L,
            messageId = "m1",
            accountId = "google_abc",
            itemType = ExtractedItemType.MEETING,
            title = "Design review",
            payload = """{"end":"$end","dateOnly":"0","tz":"Asia/Kolkata","tzSource":"EXPLICIT_IN_EMAIL","loc":"Room 5","url":"https://meet.example/abc","status":"UPCOMING","conf":"HIGH","signals":"","expl":"","ver":"1"}""",
            dueDateEpochMs = start,
            detectedAtEpochMs = 1L,
        )
    }

    @Test
    fun `executor execute creates the event and returns a receipt`() = runBlocking {
        val bodies = mutableListOf<String>()
        val transport = fakeCalendarTransport { method, path, query, body ->
            when {
                method == "GET" && path.endsWith("/users/me/calendarList") -> 200 to calendarListJson()
                method == "GET" && path.endsWith("/events") && query?.contains("privateExtendedProperty=") == true ->
                    200 to """{"items":[]}"""
                method == "POST" && path.endsWith("/events") -> {
                    bodies += body
                    200 to """{"id":"evt-42","summary":"Design review","status":"confirmed"}"""
                }
                else -> 404 to "{}"
            }
        }
            val adapter = RealCalendarIntegrationAdapter(StubAuth(), apiFor(transport))
            val executor = CalendarActionExecutor(adapter, FakeIntelligence(listOf(extractedRecord())))
            val r = executor.execute(meetingCandidate())
            val receipt = (r as MoResult.Success).value
            assertEquals("calendar", receipt.executorName)
            assertEquals("evt-42", receipt.externalId)
            assertTrue(receipt.summary.contains("Design review"))
            assertTrue(receipt.summary.contains("Personal"))
            // The posted body used the resolved temporal data faithfully.
            val sent = CalendarJson.parse(bodies.single())!!
            assertEquals("2026-10-15T10:30:00+05:30", sent.obj("start")!!.str("dateTime"))
            assertEquals("2026-10-15T11:30:00+05:30", sent.obj("end")!!.str("dateTime"))
            assertEquals("Room 5", sent.str("location"))
            assertTrue(sent.str("description")!!.contains("https://meet.example/abc"))
    }

    @Test
    fun `executor execute fails when no writable calendar exists`() = runBlocking {
        val transport = fakeCalendarTransport { method, path, _, _ ->
            if (method == "GET" && path.endsWith("/users/me/calendarList"))
                200 to """{"items":[{"id":"r","summary":"Read only","accessRole":"reader"}]}"""
            else 404 to "{}"
        }
            val adapter = RealCalendarIntegrationAdapter(StubAuth(), apiFor(transport))
            val executor = CalendarActionExecutor(adapter, FakeIntelligence(listOf(extractedRecord())))
            val r = executor.execute(meetingCandidate())
            assertTrue(r is MoResult.Failure)
    }

    @Test
    fun `executor execute fails honestly when the end time is unknown`() = runBlocking {
        val transport = fakeCalendarTransport { method, path, _, _ ->
            if (method == "GET" && path.endsWith("/users/me/calendarList")) 200 to calendarListJson()
            else 404 to "{}"
        }
            val adapter = RealCalendarIntegrationAdapter(StubAuth(), apiFor(transport))
            // No extracted item resolves -> no end time -> must not invent one.
            val executor = CalendarActionExecutor(adapter, FakeIntelligence())
            val r = executor.execute(meetingCandidate())
            assertTrue(r is MoResult.Failure)
            val msg = (r as MoResult.Failure).error.toString()
            assertTrue(msg.contains("end", ignoreCase = true))
    }
}
