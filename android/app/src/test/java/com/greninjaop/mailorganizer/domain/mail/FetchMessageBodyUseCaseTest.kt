package com.greninjaop.mailorganizer.domain.mail

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.auth.GmailApiClient
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.TransportResponse
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FetchMessageBodyUseCase tests (2026-10-13, fetch-on-open).
 *
 * Covers: short-circuit when the body is already stored (no network),
 * full fetch + persist when the body is missing, honest false when the
 * account is not connected, and honest false for unsynced messages.
 */
class FetchMessageBodyUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private fun dispatchers() = AppDispatchers(
        main = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
    )

    private val accountId = AccountId("a1")

    /** Minimal format=full payload: one text/plain part, "hello world". */
    private val fullJson = """
        {
          "id": "g1",
          "threadId": "t1",
          "labelIds": ["INBOX", "UNREAD"],
          "snippet": "hello",
          "internalDate": "1700000000000",
          "payload": {
            "mimeType": "text/plain",
            "filename": "",
            "headers": [{"name": "Subject", "value": "Test"}],
            "body": {"data": "aGVsbG8gd29ybGQ=", "size": 11}
          }
        }
    """.trimIndent()

    private fun messageRecord(
        messageId: String = "m1",
        gmailMessageId: String? = "g1",
        bodyText: String? = null,
        bodyHtml: String? = null,
    ) = MessageRecord(
        messageId = messageId,
        gmailMessageId = gmailMessageId,
        threadId = "t1",
        accountId = accountId.value,
        fromAddress = "a@example.com",
        fromName = "A",
        subject = "Test",
        snippet = "hello",
        bodyText = bodyText,
        bodyHtml = bodyHtml,
        timestampEpochMs = 1700000000000L,
    )

    /** In-memory MailRepository that actually persists body updates. */
    private inner class BodyStoringRepo(
        delegate: FakeMailRepository = FakeMailRepository(),
    ) : MailRepository by delegate {
        val stored = mutableMapOf<String, MessageRecord>()
        var updateCalls = 0

        override suspend fun getMessage(messageId: String): MessageRecord? =
            stored[messageId]

        override suspend fun updateMessageBody(
            messageId: String,
            bodyText: String?,
            bodyHtml: String?,
        ) {
            updateCalls++
            val current = stored[messageId] ?: return
            stored[messageId] = current.copy(bodyText = bodyText, bodyHtml = bodyHtml)
        }
    }

    private fun apiClient(
        token: String? = "token",
        json: String = fullJson,
        code: Int = 200,
    ): GmailApiClient {
        val auth = object : com.greninjaop.mailorganizer.data.auth.AuthRepository {
            override fun observeAuthStates(): Flow<Map<AccountId, com.greninjaop.mailorganizer.data.auth.GoogleAuthState>> =
                emptyFlow()
            override fun observeAuthState(accountId: AccountId): Flow<com.greninjaop.mailorganizer.data.auth.GoogleAuthState> =
                emptyFlow()
            override suspend fun getValidAccessToken(accountId: AccountId): String? = token
            override suspend fun handleOAuthResult(data: android.content.Intent?): com.greninjaop.mailorganizer.data.auth.AuthResult =
                throw UnsupportedOperationException()
            override fun createOAuthIntent(additionalScopes: List<String>): android.content.Intent =
                throw UnsupportedOperationException()
            override fun createIncrementalAuthIntent(
                accountId: AccountId,
                additionalScopes: List<String>,
            ): android.content.Intent = throw UnsupportedOperationException()
            override fun observeOAuthResults(): Flow<com.greninjaop.mailorganizer.data.auth.AuthResult> =
                emptyFlow()
            override suspend fun disconnect(accountId: AccountId) = Unit
            override suspend fun isConnected(accountId: AccountId): Boolean = token != null
        }
        val transport = HttpTransport { _, url, _, _ ->
            check("format=full" in url) { "expected a full-format fetch, got $url" }
            TransportResponse(code, json)
        }
        return GmailApiClient(authRepository = auth, transport = transport)
    }

    private fun useCase(
        repo: MailRepository,
        api: GmailApiClient,
    ) = FetchMessageBodyUseCase(api = api, mail = repo, dispatchers = dispatchers())

    @Test
    fun `returns true without network when body already stored`() = runTest(testDispatcher) {
        val repo = BodyStoringRepo()
        repo.stored["m1"] = messageRecord(bodyText = "already here")
        val api = apiClient()

        val result = useCase(repo, api).ensureBody(accountId, "m1")

        assertTrue(result)
        assertEquals(0, repo.updateCalls)
    }

    @Test
    fun `fetches and persists body when missing`() = runTest(testDispatcher) {
        val repo = BodyStoringRepo()
        repo.stored["m1"] = messageRecord()
        val api = apiClient()

        val result = useCase(repo, api).ensureBody(accountId, "m1")

        assertTrue(result)
        assertEquals(1, repo.updateCalls)
        assertEquals("hello world", repo.stored["m1"]?.bodyText)
    }

    @Test
    fun `returns false when account is not connected`() = runTest(testDispatcher) {
        val repo = BodyStoringRepo()
        repo.stored["m1"] = messageRecord()
        val api = apiClient(token = null)

        val result = useCase(repo, api).ensureBody(accountId, "m1")

        assertFalse(result)
        assertEquals(0, repo.updateCalls)
    }

    @Test
    fun `returns false for unsynced message without gmail id`() = runTest(testDispatcher) {
        val repo = BodyStoringRepo()
        repo.stored["m1"] = messageRecord(gmailMessageId = null)
        val api = apiClient()

        val result = useCase(repo, api).ensureBody(accountId, "m1")

        assertFalse(result)
        assertEquals(0, repo.updateCalls)
    }

    @Test
    fun `returns false when gmail reports an error`() = runTest(testDispatcher) {
        val repo = BodyStoringRepo()
        repo.stored["m1"] = messageRecord()
        val api = apiClient(code = 404, json = "{}")

        val result = useCase(repo, api).ensureBody(accountId, "m1")

        assertFalse(result)
        assertEquals(0, repo.updateCalls)
    }
}
