package com.greninjaop.mailorganizer.domain.privacy

import com.greninjaop.mailorganizer.core.privacy.DataSensitivity
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeActiveAccountPreferences : ActiveAccountPreferences {
    private val _selection = MutableStateFlow<AccountSelection>(AccountSelection.Unified)
    override val activeSelection: StateFlow<AccountSelection> = _selection.asStateFlow()

    override suspend fun setActiveSelection(selection: AccountSelection) {
        _selection.value = selection
    }
}

class PrivacyUseCaseTest {

    private lateinit var accountRepo: FakeAccountRepository
    private lateinit var activePrefs: FakeActiveAccountPreferences

    private val account1 = AccountRecord(
        accountId = "acct-1",
        emailAddress = "alex@example.com",
        displayName = "Alex Rivera",
        createdAtEpochMs = 1000L,
        connectionState = ConnectionState.CONNECTED,
    )
    private val account2 = AccountRecord(
        accountId = "acct-2",
        emailAddress = "work@company.org",
        displayName = "Alex Work",
        createdAtEpochMs = 2000L,
        connectionState = ConnectionState.CONNECTED,
    )

    @Before
    fun setUp() {
        accountRepo = FakeAccountRepository().apply {
            seed(account1, account2)
        }
        activePrefs = FakeActiveAccountPreferences()
    }

    @Test
    fun getDataInventory_coversAllRequiredCategories() {
        val testDispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(
            io = kotlinx.coroutines.Dispatchers.Unconfined,
            default = kotlinx.coroutines.Dispatchers.Unconfined,
            main = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        // PrivacyUseCase allows inspecting data inventory without full DB
        val categories = listOf(
            "Google OAuth Credentials",
            "Email Messages & Metadata",
            "Deterministic Classifications & Priorities",
            "Company & Sender Directory",
            "Full-Text Search Index",
            "Rules & User Corrections",
            "Action Cards & Proposed Tasks",
            "UI & Display Preferences",
            "Optional AI Fallback Context",
            "Advanced Automation Rules & Provenance History",
        )

        // Mockless validation of inventory items
        val useCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = testDispatchers,
        )

        val inventory = useCase.getDataInventory()
        assertEquals(10, inventory.size)
        categories.forEach { expectedCat ->
            assertTrue(
                "Missing category: $expectedCat",
                inventory.any { it.categoryName == expectedCat },
            )
        }

        // Verify credentials item specifically guarantees no plain database storage
        val credsItem = inventory.first { it.categoryName == "Google OAuth Credentials" }
        assertEquals(DataSensitivity.HIGHLY_SENSITIVE, credsItem.sensitivity)
        assertTrue(credsItem.leavesDevice)
        assertTrue(credsItem.destinationIfLeaves?.contains("google") == true)

        // Verify local message items do NOT leave device
        val messagesItem = inventory.first { it.categoryName == "Email Messages & Metadata" }
        assertFalse(messagesItem.leavesDevice)
    }

    @Test
    fun getScopeDisclosures_exposesAccurateGmailScopes() {
        val testDispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(
            io = kotlinx.coroutines.Dispatchers.Unconfined,
            default = kotlinx.coroutines.Dispatchers.Unconfined,
            main = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        val useCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = testDispatchers,
        )

        val scopes = useCase.getScopeDisclosures()
        assertTrue(scopes.any { it.scopeId == "gmail.readonly" && it.isGranted && !it.isWritePermission })
        assertTrue(scopes.any { it.scopeId == "gmail.modify" && !it.isGranted && it.isWritePermission })
    }

    @Test
    fun deleteAccountData_removesAccountAndResetsActiveSelectionIfMatching() = runTest {
        val testDispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(
            io = kotlinx.coroutines.Dispatchers.Unconfined,
            default = kotlinx.coroutines.Dispatchers.Unconfined,
            main = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        val useCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = testDispatchers,
        )

        activePrefs.setActiveSelection(AccountSelection.Single("acct-1"))
        useCase.deleteAccountData("acct-1")

        // Account is removed
        val remaining = accountRepo.observeAll().first()
        assertEquals(1, remaining.size)
        assertEquals("acct-2", remaining.first().accountId)

        // Active selection fallback to Unified
        assertEquals(AccountSelection.Unified, activePrefs.activeSelection.value)
    }

    @Test
    fun getSecurityAuditSnapshot_survivesFailingAccountRead() = runTest {
        // 2026-10-17 fix: the Privacy Overview was stuck on "Loading
        // privacy audit…" forever when the audit's account read failed
        // — the snapshot must degrade to zero accounts, never throw.
        val testDispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(
            io = kotlinx.coroutines.Dispatchers.Unconfined,
            default = kotlinx.coroutines.Dispatchers.Unconfined,
            main = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        val throwing = object : com.greninjaop.mailorganizer.data.repository.AccountRepository {
            private val delegate = accountRepo
            override suspend fun upsert(account: AccountRecord) = delegate.upsert(account)
            override suspend fun getById(accountId: String) = delegate.getById(accountId)
            override fun observeAll(): kotlinx.coroutines.flow.Flow<List<AccountRecord>> =
                kotlinx.coroutines.flow.flow { throw IllegalStateException("boom") }
            override fun observeEnabled() = delegate.observeEnabled()
            override suspend fun updateConnectionState(accountId: String, state: ConnectionState) =
                delegate.updateConnectionState(accountId, state)
            override suspend fun recordSync(accountId: String, syncEpochMs: Long) =
                delegate.recordSync(accountId, syncEpochMs)
            override suspend fun setEnabled(accountId: String, enabled: Boolean) =
                delegate.setEnabled(accountId, enabled)
            override suspend fun deleteById(accountId: String) = delegate.deleteById(accountId)
        }
        val useCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = throwing,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = testDispatchers,
        )
        val snapshot = useCase.getSecurityAuditSnapshot()
        assertEquals(0, snapshot.totalConnectedAccounts)
        assertEquals(0, snapshot.totalLocalMessagesCount)
    }

    @Test
    fun deleteAccountData_alsoClearsTokens() = runTest {
        // 2026-10-17 fix: deleting account data must also clear the
        // OAuth tokens, otherwise the auth layer restores "Connected"
        // for an account that no longer exists (split-brain).
        val testDispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(
            io = kotlinx.coroutines.Dispatchers.Unconfined,
            default = kotlinx.coroutines.Dispatchers.Unconfined,
            main = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        var disconnected: String? = null
        val auth = object : com.greninjaop.mailorganizer.data.auth.AuthRepository {
            override fun observeAuthStates() =
                kotlinx.coroutines.flow.emptyFlow<Map<com.greninjaop.mailorganizer.core.AccountId, com.greninjaop.mailorganizer.data.auth.GoogleAuthState>>()
            override fun observeAuthState(accountId: com.greninjaop.mailorganizer.core.AccountId) =
                kotlinx.coroutines.flow.emptyFlow<com.greninjaop.mailorganizer.data.auth.GoogleAuthState>()
            override suspend fun getValidAccessToken(accountId: com.greninjaop.mailorganizer.core.AccountId): String? = null
            override suspend fun handleOAuthResult(data: android.content.Intent?) =
                com.greninjaop.mailorganizer.data.auth.AuthResult.Cancelled
            override fun createOAuthIntent(additionalScopes: List<String>) =
                android.content.Intent()
            override fun createIncrementalAuthIntent(
                accountId: com.greninjaop.mailorganizer.core.AccountId,
                additionalScopes: List<String>,
            ) = android.content.Intent()
            override fun observeOAuthResults() =
                kotlinx.coroutines.flow.emptyFlow<com.greninjaop.mailorganizer.data.auth.AuthResult>()
            override suspend fun disconnect(accountId: com.greninjaop.mailorganizer.core.AccountId) {
                disconnected = accountId.value
            }
            override suspend fun isConnected(accountId: com.greninjaop.mailorganizer.core.AccountId) = false
        }
        val useCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = testDispatchers,
            authRepository = auth,
        )
        useCase.deleteAccountData("acct-1")
        assertEquals("acct-1", disconnected)
        assertEquals(1, accountRepo.observeAll().first().size)
    }
}
