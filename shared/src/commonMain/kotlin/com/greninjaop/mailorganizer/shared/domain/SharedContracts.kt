package com.greninjaop.mailorganizer.shared.domain

import com.greninjaop.mailorganizer.shared.core.AccountId
import com.greninjaop.mailorganizer.shared.core.CategoryGroup
import com.greninjaop.mailorganizer.shared.core.MailDestination
import com.greninjaop.mailorganizer.shared.core.MessageSummary
import com.greninjaop.mailorganizer.shared.core.MoResult
import com.greninjaop.mailorganizer.shared.core.SyncProgress
import com.greninjaop.mailorganizer.shared.core.ThreadSummary
import kotlinx.coroutines.flow.Flow

/**
 * Stable SwiftUI ↔ KMP boundary. Platform presentation injects these;
 * shared owns behaviour. iOS never talks to repositories directly from views.
 */
interface AccountSession {
    val activeAccountId: Flow<AccountId?>
    suspend fun listAccounts(): List<AccountSnapshot>
    suspend fun switchAccount(accountId: AccountId): MoResult<Unit>
}

data class AccountSnapshot(
    val accountId: AccountId,
    val email: String,
    val displayName: String?,
    val isActive: Boolean,
)

interface MailboxReader {
    fun observeThreads(
        accountId: AccountId,
        destination: MailDestination,
    ): Flow<List<ThreadSummary>>

    suspend fun getMessage(
        accountId: AccountId,
        messageId: String,
    ): MoResult<MessageSummary>
}

interface SyncCoordinatorPort {
    fun observeProgress(accountId: AccountId): Flow<SyncProgress>
    suspend fun startSync(accountId: AccountId): MoResult<Unit>
    suspend fun cancelSync(accountId: AccountId)
}

interface ClassificationPort {
    suspend fun categoryFor(messageId: String, accountId: AccountId): CategoryGroup
}

interface SearchPort {
    suspend fun search(accountId: AccountId, query: StructuredQuery): MoResult<List<ThreadSummary>>
}

/**
 * Structured search query — UI never builds SQL (Android Phase 21 / R39).
 */
data class StructuredQuery(
    val freeText: String? = null,
    val from: String? = null,
    val to: String? = null,
    val subject: String? = null,
    val hasAttachment: Boolean? = null,
    val isUnread: Boolean? = null,
    val isStarred: Boolean? = null,
    val afterEpochMs: Long? = null,
    val beforeEpochMs: Long? = null,
)

interface ActionEnginePort {
    suspend fun proposeActions(accountId: AccountId): List<ProposedAction>
    suspend fun execute(actionId: String, accountId: AccountId): MoResult<Unit>
}

data class ProposedAction(
    val id: String,
    val title: String,
    val rationale: String,
    val reversible: Boolean,
)
