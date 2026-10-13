package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Thread/conversation persistence (Phase 2). */
@Dao
interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(thread: ThreadRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(threads: List<ThreadRecord>)

    @Query("SELECT * FROM threads WHERE threadId = :threadId")
    suspend fun getById(threadId: String): ThreadRecord?

    /**
     * Batch thread lookup for search results (Phase 10) — one query,
     * never N+1. Empty input returns empty output.
     */
    @Query("SELECT * FROM threads WHERE threadId IN (:threadIds)")
    suspend fun getByIds(threadIds: List<String>): List<ThreadRecord>

    @Query(
        "SELECT * FROM threads WHERE accountId = :accountId " +
            "ORDER BY latestMessageEpochMs DESC LIMIT :limit",
    )
    fun observeByAccount(accountId: String, limit: Int): Flow<List<ThreadRecord>>

    /**
     * Gmail Primary (2026-10-13): inbox threads whose latest message is NOT
     * categorized as Social/Promotions and is NOT spam — the Gmail Primary
     * tab semantics. Label matching reuses the exact unit-separator pattern
     * from MessageDao's label queries.
     */
    @Query(
        "SELECT t.* FROM threads t JOIN messages m ON m.messageId = t.latestMessageId " +
            "WHERE t.accountId = :accountId " +
            "AND instr(char(31) || m.labels || char(31), char(31) || 'CATEGORY_SOCIAL' || char(31)) = 0 " +
            "AND instr(char(31) || m.labels || char(31), char(31) || 'CATEGORY_PROMOTIONS' || char(31)) = 0 " +
            "AND instr(char(31) || m.labels || char(31), char(31) || 'SPAM' || char(31)) = 0 " +
            "ORDER BY t.latestMessageEpochMs DESC LIMIT :limit",
    )
    fun observePrimaryByAccount(accountId: String, limit: Int): Flow<List<ThreadRecord>>

    /**
     * Unified Gmail Primary across enabled accounts (2026-10-13).
     */
    @Query(
        "SELECT t.* FROM threads t JOIN messages m ON m.messageId = t.latestMessageId " +
            "JOIN accounts a ON a.accountId = t.accountId " +
            "WHERE a.isEnabled = 1 " +
            "AND instr(char(31) || m.labels || char(31), char(31) || 'CATEGORY_SOCIAL' || char(31)) = 0 " +
            "AND instr(char(31) || m.labels || char(31), char(31) || 'CATEGORY_PROMOTIONS' || char(31)) = 0 " +
            "AND instr(char(31) || m.labels || char(31), char(31) || 'SPAM' || char(31)) = 0 " +
            "ORDER BY t.latestMessageEpochMs DESC LIMIT :limit",
    )
    fun observeUnifiedPrimary(limit: Int): Flow<List<ThreadRecord>>

    /**
     * Unified presentation query (Phase 18): observes threads across all
     * enabled accounts ordered by latest message epoch descending.
     */
    @Query(
        "SELECT t.* FROM threads t JOIN accounts a ON a.accountId = t.accountId " +
            "WHERE a.isEnabled = 1 ORDER BY t.latestMessageEpochMs DESC LIMIT :limit",
    )
    fun observeUnified(limit: Int): Flow<List<ThreadRecord>>

    @Query(
        "SELECT * FROM threads WHERE accountId = :accountId " +
            "ORDER BY latestMessageEpochMs DESC LIMIT :limit",
    )
    suspend fun getByAccount(accountId: String, limit: Int): List<ThreadRecord>

    @Query(
        "SELECT * FROM threads WHERE accountId = :accountId AND gmailThreadId = :gmailThreadId",
    )
    suspend fun getByGmailThreadId(accountId: String, gmailThreadId: String): ThreadRecord?

    @Query(
        "UPDATE threads SET messageCount = :messageCount, unreadCount = :unreadCount, " +
            "latestMessageId = :latestMessageId, latestMessageEpochMs = :latestMessageEpochMs, " +
            "updatedAtEpochMs = :updatedAtEpochMs WHERE threadId = :threadId",
    )
    suspend fun updateCounts(
        threadId: String,
        messageCount: Int,
        unreadCount: Int,
        latestMessageId: String?,
        latestMessageEpochMs: Long,
        updatedAtEpochMs: Long,
    )

    @Query("DELETE FROM threads WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
