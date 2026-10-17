package com.greninjaop.mailorganizer.data.integrations

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.sync.ChangePage
import com.greninjaop.mailorganizer.data.sync.DeferredGmailSyncApi
import com.greninjaop.mailorganizer.data.sync.GmailSyncApi
import com.greninjaop.mailorganizer.data.sync.MessagePage
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Adapter honesty tests (Phase 17).
 *
 * Pins the exact honest states: Gmail is never CONNECTED without OAuth,
 * deferred integrations are always UNAVAILABLE, and connect() never
 * fakes success.
 */
class IntegrationAdaptersTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun account(
        id: String = "a1",
        googleAccountId: String? = null,
    ) = AccountRecord(
        accountId = id,
        emailAddress = "user@example.com",
        displayName = "User",
        createdAtEpochMs = 0L,
        googleAccountId = googleAccountId,
        connectionState = ConnectionState.DISCONNECTED,
    )

    /** GmailSyncApi whose latestHistoryCursor succeeds (hypothetical configured client). */
    private class ConfiguredSyncApi : GmailSyncApi {
        override suspend fun fetchPage(
            accountId: AccountId,
            pageToken: String?,
            pageSize: Int,
        ): MessagePage = throw UnsupportedOperationException()
        override suspend fun fetchChanges(
            accountId: AccountId,
            historyCursor: String,
        ): ChangePage = throw UnsupportedOperationException()
        override suspend fun fetchSpamPage(
            accountId: AccountId,
            pageSize: Int,
        ): MessagePage = throw UnsupportedOperationException()
        override suspend fun latestHistoryCursor(accountId: AccountId): String = "cursor-1"
    }

    @Test
    fun `gmail with no account is disconnected`() = runTest(testDispatcher) {
        val adapter = GmailIntegrationAdapter(DeferredGmailSyncApi(), FakeAccountRepository())
        val snapshot = adapter.snapshot(null)
        assertEquals(IntegrationStatus.DISCONNECTED, snapshot.status)
        assertTrue(snapshot.usableCapabilities.isEmpty())
    }

    @Test
    fun `gmail with local-only account needs auth`() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository()
        accounts.upsert(account())
        val adapter = GmailIntegrationAdapter(DeferredGmailSyncApi(), accounts)
        val snapshot = adapter.snapshot("a1")
        assertEquals(IntegrationStatus.AUTH_REQUIRED, snapshot.status)
        assertTrue(snapshot.statusReason.isNotBlank())
        assertEquals("user@example.com", snapshot.accountEmail)
        assertTrue(snapshot.usableCapabilities.isEmpty())
    }

    @Test
    fun `gmail disconnect is a safe idempotent success`() = runTest(testDispatcher) {
        val adapter = GmailIntegrationAdapter(DeferredGmailSyncApi(), FakeAccountRepository())
        assertTrue(adapter.disconnect("a1") is MoResult.Success)
        assertTrue(adapter.disconnect(null) is MoResult.Success)
    }

    @Test
    fun `gmail never reports connected even with a configured sync seam`() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository()
        accounts.upsert(account(googleAccountId = "google-123"))
        val adapter = GmailIntegrationAdapter(ConfiguredSyncApi(), accounts)
        val snapshot = adapter.snapshot("a1")
        // Without OAuth verification the manager must not claim CONNECTED.
        assertTrue(snapshot.status != IntegrationStatus.CONNECTED)
        assertTrue(snapshot.usableCapabilities.isEmpty())
    }

    @Test
    fun `gmail declares readonly plus optional modify capabilities`() {
        val adapter = GmailIntegrationAdapter(DeferredGmailSyncApi(), FakeAccountRepository())
        assertEquals(
            setOf(
                IntegrationCapability.READ_EMAIL,
                IntegrationCapability.SYNC_EMAIL,
                // Phase 22: declared but only usable after incremental grant.
                IntegrationCapability.MODIFY_EMAIL,
            ),
            adapter.declaredCapabilities,
        )
        assertTrue(adapter.requiredPermissions.any { it.scope == "gmail.readonly" })
        val modify = adapter.requiredPermissions.single { it.scope == "gmail.modify" }
        assertTrue("gmail.modify must be optional, never bundled into sign-in", !modify.required)
        assertTrue(
            "must not declare send permissions",
            adapter.requiredPermissions.none { it.scope.contains("send", ignoreCase = true) },
        )
    }

    @Test
    fun `calendar is unavailable naming phase 15`() = runTest(testDispatcher) {
        val adapter = CalendarIntegrationAdapter()
        val snapshot = adapter.snapshot("a1")
        assertEquals(IntegrationStatus.UNAVAILABLE, snapshot.status)
        assertTrue(snapshot.statusReason.contains("Phase 15"))
        assertTrue(snapshot.usableCapabilities.isEmpty())
        assertEquals(IntegrationId.CALENDAR, snapshot.id)
    }

    @Test
    fun `tasks is unavailable naming phase 16`() = runTest(testDispatcher) {
        val adapter = TasksIntegrationAdapter()
        val snapshot = adapter.snapshot(null)
        assertEquals(IntegrationStatus.UNAVAILABLE, snapshot.status)
        assertTrue(snapshot.statusReason.contains("Phase 16"))
        assertTrue(snapshot.usableCapabilities.isEmpty())
        assertEquals(IntegrationId.TASKS, snapshot.id)
    }

    @Test
    fun `deferred disconnect is a safe no-op`() = runTest(testDispatcher) {
        assertTrue(CalendarIntegrationAdapter().disconnect("a1") is MoResult.Success)
        assertTrue(TasksIntegrationAdapter().disconnect(null) is MoResult.Success)
    }
}
