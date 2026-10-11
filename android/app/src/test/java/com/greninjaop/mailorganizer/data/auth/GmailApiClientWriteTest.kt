package com.greninjaop.mailorganizer.data.auth

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.JsonParser
import com.greninjaop.mailorganizer.core.JsonParser.array
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.TransportResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 22 unit tests: Gmail write operations against a fake Gmail API.
 *
 * Verifies request construction (endpoint, method, label payloads),
 * response verification parsing, and honest HTTP error mapping —
 * 401 → Authentication, 403 → PermissionDenied (drives the incremental
 * authorization upgrade), 404 → Api(404), 429 → RateLimited.
 *
 * Pure JVM: local HttpServer, no android.*, no Robolectric.
 */
class GmailApiClientWriteTest {

    private val accountId = AccountId("acc-1")
    data class CapturedRequest(
        val method: String,
        val path: String,
        val body: String,
        val authHeader: String?,
    )

    private val captured = mutableListOf<CapturedRequest>()
    private var nextCode: Int = 200
    private var nextBody: String = """{"id":"m1","labelIds":["INBOX"]}"""

    @After
    fun tearDown() {
        captured.clear()
    }

    private fun transportFor(token: String? = "test-token"): HttpTransport =
        HttpTransport { method, url, headers, body ->
            val path = "/" + url.substringAfter("://").substringAfter("/").substringBefore("?")
            captured += CapturedRequest(
                method = method,
                path = path,
                body = body ?: "",
                authHeader = headers["Authorization"],
            )
            val (code, response) = if (token != null && headers["Authorization"] != "Bearer $token") {
                401 to """{"error":{"code":401,"message":"Invalid credentials"}}"""
            } else {
                nextCode to nextBody
            }
            TransportResponse(code, response)
        }

    private fun client(token: String? = "test-token"): GmailApiClient {
        return GmailApiClient(
            authRepository = FakeAuth(token),
            apiBase = "http://fake.gmail",
            transport = transportFor(token),
        )
    }

    private fun labelsOf(vararg ids: String): String =
        """{"id":"m1","labelIds":[${ids.joinToString(",") { "\"$it\"" }}]}"""

    // ---------- request construction ----------

    @Test
    fun `markRead true removes UNREAD label`() = runBlocking {
        nextBody = labelsOf("INBOX")
        val result = client().markRead(accountId, "m1", true)

        assertEquals(1, captured.size)
        val req = captured[0]
        assertEquals("POST", req.method)
        assertEquals("/gmail/v1/users/me/messages/m1/modify", req.path)
        val json = JsonParser.parse(req.body)!!
        assertEquals(0, (json.array("addLabelIds")?.size ?: 0))
        assertEquals("UNREAD", ((json.array("removeLabelIds")?.get(0) as JsonParser.JsonValue.Str).value))
        assertEquals("Bearer test-token", req.authHeader)

        val verification = (result as MoResult.Success).value
        assertEquals("m1", verification.gmailMessageId)
        assertEquals(setOf("INBOX"), verification.labelIds)
    }

    @Test
    fun `markRead false adds UNREAD label`() = runBlocking {
        nextBody = labelsOf("INBOX", "UNREAD")
        val result = client().markRead(accountId, "m1", false)

        val json = JsonParser.parse(captured[0].body)!!
        assertEquals("UNREAD", ((json.array("addLabelIds")?.get(0) as JsonParser.JsonValue.Str).value))
        assertEquals(0, (json.array("removeLabelIds")?.size ?: 0))
        val verification = (result as MoResult.Success).value
        assertTrue("UNREAD" in verification.labelIds)
    }

    @Test
    fun `archive removes INBOX label`() = runBlocking {
        nextBody = labelsOf()
        val result = client().archive(accountId, "m1")

        val req = captured[0]
        assertEquals("/gmail/v1/users/me/messages/m1/modify", req.path)
        val json = JsonParser.parse(req.body)!!
        assertEquals("INBOX", ((json.array("removeLabelIds")?.get(0) as JsonParser.JsonValue.Str).value))
        val verification = (result as MoResult.Success).value
        assertFalse("INBOX" in verification.labelIds)
    }

    @Test
    fun `setStarred adds STARRED label`() = runBlocking {
        nextBody = labelsOf("INBOX", "STARRED")
        client().setStarred(accountId, "m1", true)

        val json = JsonParser.parse(captured[0].body)!!
        assertEquals("STARRED", ((json.array("addLabelIds")?.get(0) as JsonParser.JsonValue.Str).value))
    }

    @Test
    fun `setStarred false removes STARRED label`() = runBlocking {
        nextBody = labelsOf("INBOX")
        client().setStarred(accountId, "m1", false)

        val json = JsonParser.parse(captured[0].body)!!
        assertEquals("STARRED", ((json.array("removeLabelIds")?.get(0) as JsonParser.JsonValue.Str).value))
    }

    @Test
    fun `trash posts to trash endpoint`() = runBlocking {
        nextBody = labelsOf("TRASH")
        val result = client().trash(accountId, "m1")

        val req = captured[0]
        assertEquals("POST", req.method)
        assertEquals("/gmail/v1/users/me/messages/m1/trash", req.path)
        val verification = (result as MoResult.Success).value
        assertTrue("TRASH" in verification.labelIds)
    }

    @Test
    fun `untrash posts to untrash endpoint`() = runBlocking {
        nextBody = labelsOf("INBOX")
        client().untrash(accountId, "m1")

        assertEquals("/gmail/v1/users/me/messages/m1/untrash", captured[0].path)
    }

    @Test
    fun `addLabels sends label ids`() = runBlocking {
        nextBody = labelsOf("INBOX", "Label_1")
        client().addLabels(accountId, "m1", listOf("Label_1"))

        val json = JsonParser.parse(captured[0].body)!!
        assertEquals("Label_1", ((json.array("addLabelIds")?.get(0) as JsonParser.JsonValue.Str).value))
    }

    @Test
    fun `getLabelIds parses minimal response`() = runBlocking {
        nextBody = labelsOf("INBOX", "UNREAD")
        val result = client().getLabelIds(accountId, "m1")

        assertEquals("GET", captured[0].method)
        assertTrue(captured[0].path.startsWith("/gmail/v1/users/me/messages/m1"))
        assertEquals(setOf("INBOX", "UNREAD"), (result as MoResult.Success).value)
    }

    // ---------- error mapping ----------

    @Test
    fun `http 401 maps to Authentication`() = runBlocking {
        nextCode = 401
        nextBody = """{"error":{"code":401,"message":"Invalid credentials"}}"""
        val result = client().markRead(accountId, "m1", true)

        val error = (result as MoResult.Failure).error
        assertTrue(error is MoError.Authentication)
    }

    @Test
    fun `http 403 maps to PermissionDenied`() = runBlocking {
        nextCode = 403
        nextBody = """{"error":{"code":403,"message":"Insufficient Permission"}}"""
        val result = client().archive(accountId, "m1")

        val error = (result as MoResult.Failure).error
        assertTrue(error is MoError.PermissionDenied)
        assertEquals("gmail.modify", (error as MoError.PermissionDenied).permission)
    }

    @Test
    fun `http 404 maps to Api 404`() = runBlocking {
        nextCode = 404
        nextBody = """{"error":{"code":404,"message":"Not found"}}"""
        val result = client().trash(accountId, "m1")

        val error = (result as MoResult.Failure).error
        assertTrue(error is MoError.Api)
        assertEquals(404, (error as MoError.Api).code)
    }

    @Test
    fun `http 429 maps to RateLimited`() = runBlocking {
        nextCode = 429
        nextBody = """{"error":{"code":429,"message":"Rate limit exceeded"}}"""
        val result = client().markRead(accountId, "m1", true)

        assertTrue((result as MoResult.Failure).error is MoError.RateLimited)
    }

    @Test
    fun `http 500 maps to Api error`() = runBlocking {
        nextCode = 500
        nextBody = """{"error":{"code":500,"message":"Backend error"}}"""
        val result = client().markRead(accountId, "m1", true)

        val error = (result as MoResult.Failure).error
        assertTrue(error is MoError.Api)
        assertEquals(500, (error as MoError.Api).code)
    }

    @Test
    fun `missing token maps to Authentication without any http call`() = runBlocking {
        val result = client(token = null).archive(accountId, "m1")

        assertTrue((result as MoResult.Failure).error is MoError.Authentication)
        assertEquals(0, captured.size)
    }

    @Test
    fun `malformed response maps to Parsing`() = runBlocking {
        nextBody = "not-json{{{"
        val result = client().markRead(accountId, "m1", true)

        assertTrue((result as MoResult.Failure).error is MoError.Parsing)
    }

    @Test
    fun `wrong bearer token gets 401 from server`() = runBlocking {
        // Transport expects the test token; this client sends a wrong one.
        val badClient = GmailApiClient(
            authRepository = FakeAuth("wrong-token"),
            apiBase = "http://fake.gmail",
            transport = transportFor("test-token"),
        )
        val result = badClient.markRead(accountId, "m1", true)
        assertTrue((result as MoResult.Failure).error is MoError.Authentication)
    }

    // ---------- fake auth ----------

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
}
