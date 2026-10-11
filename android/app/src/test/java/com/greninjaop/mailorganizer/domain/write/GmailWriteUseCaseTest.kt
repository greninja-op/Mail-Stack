package com.greninjaop.mailorganizer.domain.write

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.AuthResult
import com.greninjaop.mailorganizer.data.auth.GoogleAuthState
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.repository.MailRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 22 unit tests: the write safety boundary.
 *
 * Proves, without any network:
 * - writes cannot execute without the confirmation flag (the §15 gate),
 * - cross-account writes are rejected (account isolation, §35–§38),
 * - idempotent no-ops make no API calls (§26),
 * - 403 → PermissionUpgradeRequired, 401 → AuthRequired, 404 → NotFound,
 * - Gmail's reported labels are verified before local state changes
 *   (§24, §25),
 * - ambiguous network failures on high-risk ops reconcile once (§30, §31),
 * - a duplicate in-flight write is rejected (§27, §28).
 *
 * Pure JVM — no android.*, no Robolectric.
 */
class GmailWriteUseCaseTest {

    private val dispatchers = AppDispatchers(
        io = Dispatchers.Unconfined,
        default = Dispatchers.Unconfined,
        main = Dispatchers.Unconfined,
    )

    private fun message(
        id: String = "local-1",
        accountId: String = "acc-A",
        gmailId: String? = "gmail-1",
        unread: Boolean = true,
        starred: Boolean = false,
        labels: List<String> = listOf("INBOX", "UNREAD"),
    ) = MessageRecord(
        messageId = id,
        gmailMessageId = gmailId,
        threadId = "thread-1",
        accountId = accountId,
        fromAddress = "someone@example.com",
        fromName = null,
        subject = "Hello",
        snippet = null,
        bodyText = null,
        timestampEpochMs = 1L,
        unread = unread,
        starred = starred,
        labels = labels,
    )

    private fun useCase(
        api: GmailWriteApi,
        mail: FakeMailRepository = FakeMailRepository(),
    ) = GmailWriteUseCase(
        writeApi = api,
        mail = mail,
        auth = FakeAuthRepository(),
        dispatchers = dispatchers,
    )

    private fun request(
        accountId: String = "acc-A",
        messageId: String = "local-1",
        operation: GmailWriteOperation = GmailWriteOperation.MARK_READ,
        confirmed: Boolean = true,
    ) = GmailWriteRequest(
        accountId = accountId,
        messageId = messageId,
        operation = operation,
        confirmed = confirmed,
    )

    // ---------- confirmation gate (§15) ----------

    @Test
    fun `unconfirmed write is rejected and never touches the api`() = runBlocking {
        val api = FakeWriteApi()
        val outcome = useCase(api).execute(request(confirmed = false))

        assertTrue(outcome is GmailWriteOutcome.Rejected)
        assertEquals(0, api.calls.get())
    }

    @Test
    fun `confirmation defaults to false`() {
        assertFalse(GmailWriteRequest("acc-A", "local-1", GmailWriteOperation.ARCHIVE).confirmed)
    }

    // ---------- account isolation (§35-§38) ----------

    @Test
    fun `cross-account write is rejected`() = runBlocking {
        val api = FakeWriteApi()
        val mail = FakeMailRepository().apply { put(message(accountId = "acc-A")) }
        val outcome = useCase(api, mail).execute(request(accountId = "acc-B"))

        assertTrue(outcome is GmailWriteOutcome.Rejected)
        assertTrue((outcome as GmailWriteOutcome.Rejected).reason.contains("Account mismatch"))
        assertEquals(0, api.calls.get())
    }

    @Test
    fun `message without gmail id is rejected`() = runBlocking {
        val api = FakeWriteApi()
        val mail = FakeMailRepository().apply { put(message(gmailId = null)) }
        val outcome = useCase(api, mail).execute(request())

        assertTrue(outcome is GmailWriteOutcome.Rejected)
        assertEquals(0, api.calls.get())
    }

    @Test
    fun `unknown message is rejected`() = runBlocking {
        val api = FakeWriteApi()
        val outcome = useCase(api, FakeMailRepository()).execute(request())

        assertTrue(outcome is GmailWriteOutcome.Rejected)
        assertEquals(0, api.calls.get())
    }

    // ---------- idempotency (§26) ----------

    @Test
    fun `already-read mark-read is a no-op`() = runBlocking {
        val api = FakeWriteApi()
        val mail = FakeMailRepository().apply {
            put(message(unread = false, labels = listOf("INBOX")))
        }
        val outcome = useCase(api, mail).execute(request())

        assertEquals(GmailWriteOutcome.Success(alreadyDone = true), outcome)
        assertEquals(0, api.calls.get())
    }

    @Test
    fun `already-archived archive is a no-op`() = runBlocking {
        val api = FakeWriteApi()
        val mail = FakeMailRepository().apply {
            put(message(unread = false, labels = listOf("UNREAD")))
        }
        val outcome = useCase(api, mail)
            .execute(request(operation = GmailWriteOperation.ARCHIVE))

        assertEquals(GmailWriteOutcome.Success(alreadyDone = true), outcome)
        assertEquals(0, api.calls.get())
    }

    // ---------- error mapping ----------

    @Test
    fun `http 403 becomes PermissionUpgradeRequired`() = runBlocking {
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            MoResult.Failure(MoError.PermissionDenied("gmail.modify"))
        })
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail).execute(request())

        assertEquals(GmailWriteOutcome.PermissionUpgradeRequired, outcome)
    }

    @Test
    fun `http 401 becomes AuthRequired`() = runBlocking {
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            MoResult.Failure(MoError.Authentication("expired"))
        })
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail).execute(request())

        assertEquals(GmailWriteOutcome.AuthRequired, outcome)
    }

    @Test
    fun `http 404 becomes NotFound`() = runBlocking {
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            MoResult.Failure(MoError.Api(404, "gone"))
        })
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail).execute(request())

        assertEquals(GmailWriteOutcome.NotFound, outcome)
    }

    // ---------- verification before local update (§24, §25) ----------

    @Test
    fun `success updates local read state`() = runBlocking {
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            MoResult.Success(WriteVerification("gmail-1", setOf("INBOX")))
        })
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail).execute(request())

        assertEquals(GmailWriteOutcome.Success(alreadyDone = false), outcome)
        assertTrue(mail.stored("local-1")!!.unread.not())
    }

    @Test
    fun `verification mismatch fails without touching local state`() = runBlocking {
        // Gmail still reports UNREAD: the write did NOT take effect.
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            MoResult.Success(WriteVerification("gmail-1", setOf("INBOX", "UNREAD")))
        })
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail).execute(request())

        assertTrue(outcome is GmailWriteOutcome.Failed)
        assertTrue(mail.stored("local-1")!!.unread) // unchanged
    }

    @Test
    fun `archive success updates local labels`() = runBlocking {
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            MoResult.Success(WriteVerification("gmail-1", setOf("UNREAD")))
        })
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail)
            .execute(request(operation = GmailWriteOperation.ARCHIVE))

        assertEquals(GmailWriteOutcome.Success(alreadyDone = false), outcome)
        assertFalse("INBOX" in mail.stored("local-1")!!.labels)
    }

    // ---------- ambiguous failure reconciliation (§30, §31) ----------

    @Test
    fun `network failure on low-risk op reports NetworkError`() = runBlocking {
        val api = FakeWriteApi(
            onWrite = { _, _, _ -> MoResult.Failure(MoError.Network("timeout")) },
        )
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail).execute(request())

        assertTrue(outcome is GmailWriteOutcome.NetworkError)
        assertTrue(mail.stored("local-1")!!.unread) // unchanged
    }

    @Test
    fun `network failure on archive reconciles when write landed`() = runBlocking {
        val api = FakeWriteApi(
            // The write itself hit a transport failure…
            onWrite = { _, _, _ -> MoResult.Failure(MoError.Network("timeout")) },
            // …but reconciliation shows INBOX already gone: it landed.
            onGetLabels = { _, _ -> MoResult.Success(setOf("UNREAD")) },
        )
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail)
            .execute(request(operation = GmailWriteOperation.ARCHIVE))

        assertEquals(GmailWriteOutcome.Success(alreadyDone = false), outcome)
        assertFalse("INBOX" in mail.stored("local-1")!!.labels)
    }

    @Test
    fun `network failure on archive reports safe retry when not applied`() = runBlocking {
        val api = FakeWriteApi(
            onWrite = { _, _, _ -> MoResult.Failure(MoError.Network("timeout")) },
            onGetLabels = { _, _ -> MoResult.Success(setOf("INBOX", "UNREAD")) },
        )
        val mail = FakeMailRepository().apply { put(message()) }
        val outcome = useCase(api, mail)
            .execute(request(operation = GmailWriteOperation.ARCHIVE))

        assertTrue(outcome is GmailWriteOutcome.NetworkError)
        assertTrue("INBOX" in mail.stored("local-1")!!.labels) // unchanged
    }

    // ---------- double execution guard (§27, §28) ----------

    @Test
    fun `duplicate in-flight write is rejected`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            MoResult.Success(WriteVerification("gmail-1", setOf("INBOX")))
        })
        val mail = FakeMailRepository().apply { put(message()) }
        val uc = useCase(api, mail)

        runBlocking {
            // The first execute must run off the test thread: it blocks
            // inside the fake API until released.
            val first = async(Dispatchers.Default) { uc.execute(request()) }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            val second = uc.execute(request())
            assertTrue(second is GmailWriteOutcome.Rejected)
            assertTrue(
                (second as GmailWriteOutcome.Rejected).reason.contains("already running"),
            )
            release.countDown()
            assertTrue(first.await() is GmailWriteOutcome.Success)
        }
    }

    // ---------- bulk (§47) ----------

    @Test
    fun `bulk executes per item keeping accounts separate`() = runBlocking {
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            MoResult.Success(WriteVerification("gmail-x", setOf("INBOX")))
        })
        val mail = FakeMailRepository().apply {
            put(message(id = "m-a", accountId = "acc-A", gmailId = "gmail-a"))
            put(message(id = "m-b", accountId = "acc-B", gmailId = "gmail-b"))
        }
        val uc = useCase(api, mail)
        val results = uc.executeBulk(
            listOf(
                request(accountId = "acc-A", messageId = "m-a"),
                request(accountId = "acc-B", messageId = "m-b"),
            ),
        )

        assertEquals(2, results.size)
        assertTrue(results.all { it.second is GmailWriteOutcome.Success })
        assertEquals(2, api.calls.get())
        // Each write used its own account credential.
        assertEquals(setOf("acc-A", "acc-B"), api.usedAccounts.toSet())
    }

    @Test
    fun `bulk still requires confirmation per item`() = runBlocking {
        val api = FakeWriteApi(onWrite = { _, _, _ ->
            MoResult.Success(WriteVerification("gmail-a", setOf("INBOX")))
        })
        val mail = FakeMailRepository().apply {
            put(message(id = "m-a", gmailId = "gmail-a"))
        }
        val results = useCase(api, mail).executeBulk(
            listOf(request(messageId = "m-a", confirmed = false)),
        )

        assertTrue(results[0].second is GmailWriteOutcome.Rejected)
        assertEquals(0, api.calls.get())
    }

    // ---------- fakes ----------

    private class FakeWriteApi(
        private val onWrite: (AccountId, String, String) -> MoResult<WriteVerification> = { _, _, _ ->
            MoResult.Success(WriteVerification("gmail-1", emptySet()))
        },
        private val onGetLabels: (AccountId, String) -> MoResult<Set<String>> = { _, _ ->
            MoResult.Success(emptySet())
        },
    ) : GmailWriteApi {
        val calls = AtomicInteger(0)
        val usedAccounts = mutableListOf<String>()

        private fun record(accountId: AccountId, op: String): MoResult<WriteVerification> {
            calls.incrementAndGet()
            usedAccounts += accountId.value
            return onWrite(accountId, op, "")
        }

        override suspend fun markRead(
            accountId: AccountId,
            gmailMessageId: String,
            read: Boolean,
        ) = record(accountId, "markRead")

        override suspend fun setStarred(
            accountId: AccountId,
            gmailMessageId: String,
            starred: Boolean,
        ) = record(accountId, "setStarred")

        override suspend fun archive(
            accountId: AccountId,
            gmailMessageId: String,
        ) = record(accountId, "archive")

        override suspend fun trash(
            accountId: AccountId,
            gmailMessageId: String,
        ) = record(accountId, "trash")

        override suspend fun untrash(
            accountId: AccountId,
            gmailMessageId: String,
        ) = record(accountId, "untrash")

        override suspend fun addLabels(
            accountId: AccountId,
            gmailMessageId: String,
            labelIds: List<String>,
        ) = record(accountId, "addLabels")

        override suspend fun removeLabels(
            accountId: AccountId,
            gmailMessageId: String,
            labelIds: List<String>,
        ) = record(accountId, "removeLabels")

        override suspend fun getLabelIds(
            accountId: AccountId,
            gmailMessageId: String,
        ): MoResult<Set<String>> = onGetLabels(accountId, gmailMessageId)
    }

    private class FakeMailRepository : MailRepository {
        private val messages = mutableMapOf<String, MessageRecord>()

        fun put(record: MessageRecord) {
            messages[record.messageId] = record
        }

        fun stored(id: String): MessageRecord? = messages[id]

        override suspend fun getMessage(messageId: String): MessageRecord? = messages[messageId]

        override suspend fun setRead(messageId: String, read: Boolean) {
            messages[messageId] = messages[messageId]!!.copy(unread = !read)
        }

        override suspend fun setStarred(messageId: String, starred: Boolean) {
            messages[messageId] = messages[messageId]!!.copy(starred = starred)
        }

        override suspend fun setLabels(messageId: String, labels: List<String>) {
            messages[messageId] = messages[messageId]!!.copy(labels = labels)
        }

        // Everything below is unused by the write use case.
        override suspend fun saveThreadWithMessages(
            thread: ThreadRecord,
            messages: List<MessageRecord>,
        ) = throw UnsupportedOperationException()
        override fun observeThreads(accountId: String, limit: Int) = emptyFlow<List<ThreadRecord>>()
        override fun observeMessages(threadId: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override fun observeUnread(accountId: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override fun observeStarred(accountId: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override suspend fun searchByText(accountId: String, query: String, limit: Int) =
            throw UnsupportedOperationException()
        override suspend fun countByAccount(accountId: String) =
            throw UnsupportedOperationException()
        override suspend fun getMessagesByIds(messageIds: List<String>) =
            throw UnsupportedOperationException()
        override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String) =
            throw UnsupportedOperationException()
        override suspend fun updateThreadAggregates(threadId: String) =
            throw UnsupportedOperationException()
        override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>) =
            throw UnsupportedOperationException()
        override suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>) =
            throw UnsupportedOperationException()
        override suspend fun getUnclassifiedMessages(accountId: String, limit: Int) =
            throw UnsupportedOperationException()
        override suspend fun getUnprioritizedMessages(accountId: String, limit: Int) =
            throw UnsupportedOperationException()
        override suspend fun getUnextractedMessages(accountId: String, limit: Int) =
            throw UnsupportedOperationException()
        override fun observeByLabel(accountId: String, label: String, limit: Int) =
            emptyFlow<List<MessageRecord>>()
        override suspend fun setMessageCompanyId(messageId: String, companyId: String?) =
            throw UnsupportedOperationException()
        override suspend fun getMessagesWithoutCompany(accountId: String, limit: Int) =
            throw UnsupportedOperationException()
        override fun observeMessagesByCompany(accountId: String, companyId: String, limit: Int) =
            emptyFlow<List<MessageRecord>>()
        override fun observeMessagesByCompanyAndCategory(
            accountId: String,
            category: MailCategory,
            companyId: String,
            limit: Int,
        ) = emptyFlow<List<MessageRecord>>()
        override fun observeMessagesByCompanyAndLabel(
            accountId: String,
            companyId: String,
            label: String,
            limit: Int,
        ) = emptyFlow<List<MessageRecord>>()
        override suspend fun companyCountsForCategory(
            accountId: String,
            category: MailCategory,
        ): Map<String, Int> = throw UnsupportedOperationException()
        override suspend fun companyCountsForLabel(
            accountId: String,
            label: String,
        ): Map<String, Int> = throw UnsupportedOperationException()
        override suspend fun companyMessageCounts(accountId: String): Map<String, Int> =
            throw UnsupportedOperationException()
        override suspend fun getMessageIdsByAccount(accountId: String, limit: Int) =
            throw UnsupportedOperationException()
        override suspend fun getMessageIdsBySender(accountId: String, email: String, limit: Int) =
            throw UnsupportedOperationException()
        override suspend fun getMessageIdsByDomain(accountId: String, domain: String, limit: Int) =
            throw UnsupportedOperationException()
        override suspend fun getMessageIdsByCompany(accountId: String, companyId: String, limit: Int) =
            throw UnsupportedOperationException()
    }

    private class FakeAuthRepository : AuthRepository {
        var incrementalScopes: List<String>? = null
        override fun observeAuthStates(): Flow<Map<AccountId, GoogleAuthState>> = flowOf(emptyMap())
        override fun observeAuthState(accountId: AccountId): Flow<GoogleAuthState> =
            flowOf(GoogleAuthState.Disconnected)
        override suspend fun getValidAccessToken(accountId: AccountId): String? = "token"
        override suspend fun handleOAuthResult(data: android.content.Intent?): AuthResult =
            throw UnsupportedOperationException()
        override fun createOAuthIntent(additionalScopes: List<String>): android.content.Intent =
            throw UnsupportedOperationException()
        override fun createIncrementalAuthIntent(
            accountId: AccountId,
            additionalScopes: List<String>,
        ): android.content.Intent {
            incrementalScopes = additionalScopes
            throw UnsupportedOperationException("not needed in these tests")
        }
        override fun observeOAuthResults(): Flow<AuthResult> = flowOf()
        override suspend fun disconnect(accountId: AccountId) = Unit
        override suspend fun isConnected(accountId: AccountId): Boolean = true
    }
}
