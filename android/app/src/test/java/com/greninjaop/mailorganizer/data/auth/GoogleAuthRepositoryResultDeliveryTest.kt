package com.greninjaop.mailorganizer.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.repository.RoomAccountRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * OAuth result delivery (2026-10-17 fix): the redirect can arrive on a
 * cold start (process killed while the browser was in front) and the
 * Connect ViewModel is created after the exchange finishes. With a
 * no-replay flow the result was lost, the Connect screen stayed on
 * "Connect with Google", and tapping it opened Chrome all over again
 * (the Connect → Chrome loop). The last result must replay to a late
 * subscriber, a redelivery must re-emit, and consuming it must clear
 * the replay so a later Connect session gets nothing stale.
 *
 * These tests use the no-pending-flow path (no OAuth flow was started),
 * so no network is involved: handleOAuthResult returns Failure(UNKNOWN)
 * and emits it — the delivery mechanism is identical for Success.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GoogleAuthRepositoryResultDeliveryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: GoogleAuthRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dispatchers = com.greninjaop.mailorganizer.core.AppDispatchers()
        repo = GoogleAuthRepository(
            tokenStorage = SecureTokenStorage(context),
            accountRepository = RoomAccountRepository(db.accountDao(), dispatchers),
            oauthClientId = "test-client-id",
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun redirectIntent() = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("com.greninjaop.mailorganizer:/oauth2redirect?state=s1&code=c1"),
    )

    @Test
    fun `late subscriber receives replayed oauth result`() = runBlocking {
        val first = repo.handleOAuthResult(redirectIntent())
        assertTrue(first is AuthResult.Failure)

        // No collector was subscribed when the result was emitted — a
        // late subscriber must still get it (replay = 1).
        val replayed = repo.observeOAuthResults().first()
        assertEquals(first, replayed)
    }

    @Test
    fun `redelivery re-emits the cached result`() = runBlocking {
        val first = repo.handleOAuthResult(redirectIntent())
        assertTrue(first is AuthResult.Failure)

        // A collector that already received the replay gets the
        // redelivery as a second emission.
        val seen = mutableListOf<AuthResult>()
        val job = launch {
            repo.observeOAuthResults().collect { seen.add(it) }
        }
        // Let the collector subscribe + receive the replay.
        kotlinx.coroutines.delay(100)
        assertEquals(listOf(first), seen)
        repo.handleOAuthResult(redirectIntent())
        kotlinx.coroutines.delay(100)
        assertEquals(listOf(first, first), seen)
        job.cancel()
    }

    @Test
    fun `consumed result is not replayed to a later subscriber`() = runBlocking {
        val first = repo.handleOAuthResult(redirectIntent())
        assertTrue(first is AuthResult.Failure)
        // A subscriber consumes it (what ConnectAccountViewModel does).
        assertEquals(first, repo.observeOAuthResults().first())
        repo.clearOAuthResult()

        // A later subscriber (e.g. opening Connect to add a second
        // account) must get nothing stale.
        val none = withTimeoutOrNull(300) { repo.observeOAuthResults().first() }
        assertNull(none)
    }
}
