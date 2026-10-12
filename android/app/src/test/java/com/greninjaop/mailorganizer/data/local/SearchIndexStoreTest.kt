package com.greninjaop.mailorganizer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.repository.RoomAccountRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Crash-repair regression tests (2026-10-12): removing an account crashed the
 * app when the FTS table did not exist. [SearchIndexStore.createTableOrSkip]
 * legitimately skips table creation on devices whose SQLite lacks the FTS5
 * module; the raw `DELETE FROM messages_fts` in the store then threw
 * "no such table", escaping [com.greninjaop.mailorganizer.ui.settings.AccountsViewModel.disconnectAccount]'s
 * coroutine and killing the app before the account row was deleted.
 *
 * Runs on the JVM via Robolectric (no emulator needed).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SearchIndexStoreTest {

    private lateinit var db: AppDatabase
    private lateinit var store: SearchIndexStore
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val writable = db.openHelper.writableDatabase
        // Simulate a device where FTS5 creation was skipped: no table.
        writable.execSQL("DROP TABLE IF EXISTS ${SearchIndexStore.TABLE}")
        store = SearchIndexStore(writable)
        assertFalse(store.tableExists())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `deleteForAccount does not throw when FTS table is missing`() {
        store.deleteForAccount("acc-1")
    }

    @Test
    fun `deleteDocument does not throw when FTS table is missing`() {
        store.deleteDocument("msg-1")
    }

    @Test
    fun `clearAll does not throw when FTS table is missing`() {
        store.clearAll()
    }

    @Test
    fun `account deletion completes when FTS table is missing`() = runTest(testDispatcher) {
        val dispatchers = AppDispatchers(
            io = testDispatcher,
            default = testDispatcher,
            main = testDispatcher,
        )
        val repo = RoomAccountRepository(
            accountDao = db.accountDao(),
            dispatchers = dispatchers,
            searchIndex = store,
        )
        repo.upsert(AccountRecord("acc-1", "a@example.com", null, 1L))

        // Must not throw, and the account row must actually be removed.
        repo.deleteById("acc-1")

        assertNull(repo.getById("acc-1"))
    }

    @Test
    fun `deleteForAccount removes only that account's documents when table exists`() {
        // Positive control — only meaningful where FTS5 is available.
        if (!SearchIndexStore.createTableOrSkip(db.openHelper.writableDatabase)) return
        assertTrue(store.tableExists())

        store.indexDocument(
            SearchIndexStore.FtsDocument(
                messageId = "m-1", accountId = "acc-1", subject = "hello",
                bodyText = null, fromName = null, fromAddress = "a@example.com",
                snippet = null, companyName = null, labelsText = "INBOX",
            ),
        )
        store.indexDocument(
            SearchIndexStore.FtsDocument(
                messageId = "m-2", accountId = "acc-2", subject = "world",
                bodyText = null, fromName = null, fromAddress = "b@example.com",
                snippet = null, companyName = null, labelsText = "INBOX",
            ),
        )

        store.deleteForAccount("acc-1")

        assertEquals(0, store.countForAccount("acc-1"))
        assertEquals(1, store.countForAccount("acc-2"))
    }
}
