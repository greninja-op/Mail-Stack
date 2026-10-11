package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.JsonParser
import com.greninjaop.mailorganizer.core.JsonParser.array
import com.greninjaop.mailorganizer.core.JsonParser.bool
import com.greninjaop.mailorganizer.core.JsonParser.long
import com.greninjaop.mailorganizer.core.JsonParser.obj
import com.greninjaop.mailorganizer.core.JsonParser.string
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.AuthResult
import com.greninjaop.mailorganizer.data.auth.GoogleAuthState
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.TransportResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the real Gmail sync API client (Phase 3/4 follow-up):
 * JSON parsing, message mapping, address parsing, error mapping, and the
 * full HTTP flow against a local fake Gmail server.
 *
 * Pure JVM — no android.jar stubs, no Robolectric.
 */
class HttpGmailSyncApiTest {

    // ---------- JsonParser ----------

    @Test
    fun `parser reads nested objects and arrays`() {
        val v = JsonParser.parse(
            """{"a":{"b":[1,2,{"c":"x"}]},"d":true,"e":null}""",
        )
        assertNotNull(v)
        val b = v!!.obj("a")?.array("b")
        assertEquals(3, b?.size)
        assertEquals("x", (b?.get(2) as? JsonParser.JsonValue.Obj)?.string("c"))
        assertEquals(true, v.bool("d"))
    }

    @Test
    fun `parser bool and null`() {
        val v = JsonParser.parse("""{"t":true,"f":false,"n":null}""")!!
        assertEquals(true, v.bool("t"))
        assertEquals(false, v.bool("f"))
        assertEquals(null, v.bool("n"))
    }

    @Test
    fun `parser numbers`() {
        val v = JsonParser.parse("""{"i":1728123456789,"d":1.5,"e":1e3}""")!!
        assertEquals(1728123456789L, v.long("i"))
    }

    @Test
    fun `parser rejects malformed`() {
        assertNull(JsonParser.parse(""))
        assertNull(JsonParser.parse("{"))
        assertNull(JsonParser.parse("""{"a":}"""))
        assertNull(JsonParser.parse("""{"a":1"""))
        assertNull(JsonParser.parse("[1,2"))
        assertNull(JsonParser.parse("""{"a":"unterminated}"""))
        assertNull(JsonParser.parse("{\"a\": 01}"))
    }

    @Test
    fun `parser handles escapes and unicode`() {
        val v = JsonParser.parse("""{"s":"a\nb\"c\\dé"}""")!!
        assertEquals("a\nb\"c\\d\u00e9", v.string("s"))
    }

    // ---------- Address parsing ----------

    @Test
    fun `parseAddress handles display name with brackets`() {
        val (name, addr) = HttpGmailSyncApi.parseAddress("Alice Wonder <alice@example.com>")
        assertEquals("Alice Wonder", name)
        assertEquals("alice@example.com", addr)
    }

    @Test
    fun `parseAddress handles quoted display name`() {
        val (name, addr) = HttpGmailSyncApi.parseAddress("\"Doe, John\" <john@example.com>")
        assertEquals("Doe, John", name)
        assertEquals("john@example.com", addr)
    }

    @Test
    fun `parseAddress handles bare address`() {
        val (name, addr) = HttpGmailSyncApi.parseAddress("bob@example.com")
        assertNull(name)
        assertEquals("bob@example.com", addr)
    }

    @Test
    fun `parseAddress handles empty`() {
        val (name, addr) = HttpGmailSyncApi.parseAddress("  ")
        assertNull(name)
        assertEquals("", addr)
    }

    // ---------- Message mapping ----------

    private fun sampleMessageJson(): JsonParser.JsonValue = JsonParser.parse(
        """{
          "id": "18c123",
          "threadId": "18c123",
          "labelIds": ["INBOX", "UNREAD"],
          "snippet": "Hello there",
          "internalDate": "1728123456789",
          "sizeEstimate": 4096,
          "payload": {"headers": [
            {"name": "From", "value": "Alice <alice@example.com>"},
            {"name": "To", "value": "bob@example.com"},
            {"name": "Subject", "value": "Test subject"},
            {"name": "Date", "value": "Sat, 05 Oct 2026 10:00:00 +0530"}
          ]}
        }""",
    )!!

    @Test
    fun `parseMetadataMessage maps all fields`() {
        val msg = HttpGmailSyncApi.parseMetadataMessage(sampleMessageJson(), "18c123", "18c123")
        assertNotNull(msg)
        msg!!
        assertEquals("18c123", msg.id)
        assertEquals("alice@example.com", msg.fromAddress)
        assertEquals("Alice", msg.fromName)
        assertEquals(listOf("bob@example.com"), msg.toAddresses)
        assertEquals("Test subject", msg.subject)
        assertEquals("Hello there", msg.snippet)
        assertEquals(1728123456789L, msg.timestampEpochMs)
        assertTrue(msg.unread)
        assertTrue(!msg.starred)
        assertEquals(listOf("INBOX", "UNREAD"), msg.labels)
        assertEquals(4096L, msg.sizeBytes)
    }

    @Test
    fun `parseMetadataMessage returns null without From`() {
        val json = JsonParser.parse(
            """{"id":"x","threadId":"y","payload":{"headers":[{"name":"Subject","value":"s"}]}}""",
        )!!
        assertNull(HttpGmailSyncApi.parseMetadataMessage(json, "x", "y"))
    }

    @Test
    fun `parseMetadataMessage detects starred`() {
        val json = JsonParser.parse(
            """{"id":"x","threadId":"y","labelIds":["STARRED"],"internalDate":"1",
               "payload":{"headers":[{"name":"From","value":"a@b.c"}]}}""",
        )!!
        val msg = HttpGmailSyncApi.parseMetadataMessage(json, "x", "y")!!
        assertTrue(msg.starred)
        assertTrue(!msg.unread)
    }

    // ---------- Fake auth ----------

    private class FakeAuth(private val token: String?) : AuthRepository {
        override fun observeAuthStates(): Flow<Map<AccountId, GoogleAuthState>> = flowOf(emptyMap())
        override fun observeAuthState(accountId: AccountId): Flow<GoogleAuthState> =
            flowOf(GoogleAuthState.Disconnected)
        override suspend fun getValidAccessToken(accountId: AccountId): String? = token
        override suspend fun handleOAuthResult(data: android.content.Intent?): AuthResult =
            throw UnsupportedOperationException()
        override fun createOAuthIntent(additionalScopes: List<String>): android.content.Intent =
            throw UnsupportedOperationException()
        override fun createIncrementalAuthIntent(
            accountId: AccountId,
            additionalScopes: List<String>,
        ): android.content.Intent = throw UnsupportedOperationException()
        override fun observeOAuthResults(): Flow<AuthResult> = flowOf()
        override suspend fun disconnect(accountId: AccountId) = Unit
        override suspend fun isConnected(accountId: AccountId): Boolean = token != null
    }

    private fun fakeGmailTransport(
        handler: (path: String, query: String?) -> Pair<Int, String>,
    ): HttpTransport = HttpTransport { _, url, headers, _ ->
        val afterHost = url.substringAfter("://").substringAfter("/")
        val path = "/" + afterHost.substringBefore("?")
        val query = afterHost.substringAfter("?", "").ifEmpty { null }
        val (code, response) = if (headers["Authorization"] != "Bearer test-token") {
            401 to """{"error":{"code":401,"message":"Invalid credentials","reason":"authError"}}"""
        } else {
            handler(path, query)
        }
        TransportResponse(code, response)
    }

    private fun apiFor(
        handler: (path: String, query: String?) -> Pair<Int, String>,
        auth: AuthRepository = FakeAuth("test-token"),
    ): HttpGmailSyncApi = HttpGmailSyncApi(
        authRepository = auth,
        apiBase = "http://fake.gmail",
        transport = fakeGmailTransport(handler),
    )
    private fun messageGetJson(id: String): String =
        """{"id":"$id","threadId":"t-$id","labelIds":["INBOX","UNREAD"],
           "snippet":"snip $id","internalDate":"1728123456789","sizeEstimate":100,
           "payload":{"headers":[
             {"name":"From","value":"Sender <$id@example.com>"},
             {"name":"Subject","value":"Subject $id"}]}}"""

    // ---------- Live flow tests ----------

    @Test
    fun `fetchPage lists and fetches messages`() = runBlocking {
        val api = apiFor({ path, _ ->
            when {
                path == "/gmail/v1/users/me/messages" ->
                    200 to """{"messages":[{"id":"m1","threadId":"t1"},{"id":"m2","threadId":"t2"}],
                               "nextPageToken":"tok123","resultSizeEstimate":2}"""
                path.startsWith("/gmail/v1/users/me/messages/") -> {
                    val id = path.substringAfterLast("/")
                    200 to messageGetJson(id)
                }
                else -> 404 to "{}"
            }
        }, auth = FakeAuth("test-token"))
            val page = api.fetchPage(AccountId("a1"), null, 10)
            assertEquals(2, page.messages.size)
            assertEquals("tok123", page.nextPageToken)
            assertEquals("m1@example.com", page.messages[0].fromAddress)
            assertEquals("Subject m1", page.messages[0].subject)
    }

    @Test
    fun `fetchPage without token fails closed`() = runBlocking {
        val api = apiFor({ _, _ -> 200 to "{}" }, auth = FakeAuth(null))
        try {
            api.fetchPage(AccountId("a1"), null, 10)
            throw AssertionError("expected NotConfigured")
        } catch (e: SyncApiException.NotConfigured) {
            // expected
        }
    }

    @Test
    fun `fetchPage maps 401 to AuthExpired`() = runBlocking {
        // Wrong token -> transport returns 401.
        val api = apiFor({ _, _ -> 200 to "{}" }, auth = FakeAuth("wrong-token"))
        try {
            api.fetchPage(AccountId("a1"), null, 10)
            throw AssertionError("expected AuthExpired")
        } catch (e: SyncApiException.AuthExpired) {
            // expected
        }
    }

    @Test
    fun `latestHistoryCursor returns historyId`() = runBlocking {
        val api = apiFor({ path, _ ->
            if (path == "/gmail/v1/users/me/profile") {
                200 to """{"emailAddress":"u@example.com","historyId":"98765"}"""
            } else 404 to "{}"
        }, auth = FakeAuth("test-token"))
            assertEquals("98765", api.latestHistoryCursor(AccountId("a1")))
    }

    @Test
    fun `fetchChanges parses added and deleted`() = runBlocking {
        val api = apiFor({ path, _ ->
            when {
                path == "/gmail/v1/users/me/history" ->
                    200 to """{"historyId":"99999","history":[
                      {"messagesAdded":[{"message":{"id":"m9","threadId":"t9"}}]},
                      {"messagesDeleted":[{"message":{"id":"m_old","threadId":"t_old"}}]},
                      {"labelsAdded":[{"message":{"id":"m8","threadId":"t8"},"labelIds":["STARRED"]}]}]}"""
                path.startsWith("/gmail/v1/users/me/messages/") -> {
                    val id = path.substringAfterLast("/")
                    200 to messageGetJson(id)
                }
                else -> 404 to "{}"
            }
        }, auth = FakeAuth("test-token"))
            val changes = api.fetchChanges(AccountId("a1"), "11111")
            assertEquals("99999", changes.nextHistoryCursor)
            assertEquals(2, changes.messages.size) // m9 + m8 (label change re-fetched)
            assertEquals(listOf("m_old"), changes.deletedRemoteIds)
    }

    @Test
    fun `fetchChanges maps 404 to HistoryInvalid`() = runBlocking {
        val api = apiFor({ path, _ ->
            if (path == "/gmail/v1/users/me/history") {
                404 to """{"error":{"code":404,"message":"historyId not found"}}"""
            } else 404 to "{}"
        }, auth = FakeAuth("test-token"))
            try {
                api.fetchChanges(AccountId("a1"), "stale-cursor")
                throw AssertionError("expected HistoryInvalid")
            } catch (e: SyncApiException.HistoryInvalid) {
                // expected
            }
    }

    @Test
    fun `fetchPage maps 429 to RateLimited`() = runBlocking {
        val api = apiFor({ _, _ -> 429 to "{}" })
        try {
            api.fetchPage(AccountId("a1"), null, 10)
            throw AssertionError("expected RateLimited")
        } catch (e: SyncApiException.RateLimited) {
            // expected
        }
    }
}
