package com.greninjaop.mailorganizer.data.integrations

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.integrations.IntegrationAdapter
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.core.integrations.PermissionDescription
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.GoogleAuthState
import com.greninjaop.mailorganizer.data.tasks.GoogleTasksHttp
import com.greninjaop.mailorganizer.data.tasks.TaskExecutionStore
import com.greninjaop.mailorganizer.data.tasks.TasksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Google Tasks integration adapter — Phase 16 (real implementation).
 *
 * Connects Mail Organizer's action intelligence to Google Tasks
 * via the official Tasks API (`GoogleTasksHttp` + `TasksRepository`).
 *
 * Core principle (Phase 16):
 * ```
 * Email → Action Intelligence → Task Proposal
 *   → User Review → Explicit Confirmation
 *   → Google Tasks API → Verified Result
 * ```
 *
 * NEVER creates tasks without explicit user confirmation — that gate
 * lives in the Action Engine (`ReviewActionUseCase`); this adapter only
 * reports honest status and executes through `TasksRepository` when the
 * engine's executor asks.
 *
 * Honesty (phase §29): [IntegrationStatus.CONNECTED] is reported ONLY
 * when a lightweight API probe proves a usable token *and* the tasks
 * scope exist. A mere local token is not enough — the scope may be
 * missing (sign-in predates the scope), revoked, or expired.
 */
class RealTasksIntegrationAdapter(
    private val authRepository: AuthRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    tasksHttp: GoogleTasksHttp = GoogleTasksHttp(),
    executionStore: TaskExecutionStore? = null,
) : IntegrationAdapter {

    override val id: IntegrationId = IntegrationId.TASKS
    override val displayName: String = "Google Tasks"
    override val provider: String = "Google"

    override val declaredCapabilities: Set<IntegrationCapability> = setOf(
        IntegrationCapability.READ_TASK_LISTS,
        IntegrationCapability.CREATE_TASK,
    )

    override val requiredPermissions: List<PermissionDescription> = listOf(
        PermissionDescription(
            scope = "https://www.googleapis.com/auth/tasks",
            purpose = "Create tasks you explicitly choose from your mail. " +
                "Mailstack never creates tasks on its own.",
            required = true,
        ),
    )

    /**
     * The Tasks use-case layer. Exposed so `AppContainer` can hand the
     * same instance to the Action Engine's `CreateTaskActionExecutor`
     * without building a second repository.
     */
    val tasksRepository: TasksRepository =
        TasksRepository(authRepository, tasksHttp, executionStore, clock)

    override suspend fun snapshot(accountId: String?): IntegrationSnapshot =
        withContext(Dispatchers.IO) {
            try {
                val (status, reason) = probe(accountId)
                IntegrationSnapshot(
                    id = id,
                    displayName = displayName,
                    provider = provider,
                    accountId = accountId,
                    accountEmail = resolveEmail(accountId),
                    status = status,
                    statusReason = reason,
                    declaredCapabilities = declaredCapabilities,
                    requiredPermissions = requiredPermissions,
                    lastCheckedEpochMs = clock(),
                )
            } catch (t: Throwable) {
                // Total: never throw (contract).
                MoLogger.w(TAG, "snapshot failed: ${t.javaClass.simpleName}")
                IntegrationSnapshot(
                    id = id,
                    displayName = displayName,
                    provider = provider,
                    accountId = accountId,
                    accountEmail = null,
                    status = IntegrationStatus.ERROR,
                    statusReason = "Couldn't check Google Tasks status.",
                    declaredCapabilities = declaredCapabilities,
                    requiredPermissions = requiredPermissions,
                    lastCheckedEpochMs = clock(),
                )
            }
        }

    /**
     * Idempotent disconnect (phase §18): clears this account's local
     * Tasks metadata (selected list, execution records) only. Never
     * touches mail, actions, or other integrations. The Google grant
     * itself is revoked via the account-level disconnect in
     * `AuthRepository` — this adapter never claims local disconnect
     * equals Google revocation (phase §23).
     */
    override suspend fun disconnect(accountId: String?): MoResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (!accountId.isNullOrBlank()) {
                    tasksRepository.clearAccountData(AccountId(accountId))
                }
                MoResult.Success(Unit)
            } catch (t: Throwable) {
                MoLogger.w(TAG, "disconnect failed: ${t.javaClass.simpleName}")
                MoResult.Success(Unit)
            }
        }

    override suspend fun refresh(accountId: String?): IntegrationSnapshot =
        snapshot(accountId)

    /**
     * Honest status probe (phase §29, §35–§38):
     * - no account in scope → DISCONNECTED
     * - no usable Google auth → AUTH_REQUIRED
     * - probe 200 → CONNECTED (token + tasks scope proven)
     * - probe 401 → AUTH_REQUIRED (expired/revoked; refresh already tried)
     * - probe 403/insufficient scope → PERMISSION_REQUIRED (sign in
     *   again to grant the scope; Gmail/Calendar keep working)
     * - network failure → OFFLINE (not a disconnect)
     * - anything else → ERROR
     */
    private suspend fun probe(accountId: String?): Pair<IntegrationStatus, String> {
        if (accountId.isNullOrBlank()) {
            return IntegrationStatus.DISCONNECTED to
                "Select an account to use Google Tasks."
        }
        val accId = AccountId(accountId)
        val authed = try {
            authRepository.isConnected(accId)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "isConnected failed: ${t.javaClass.simpleName}")
            false
        }
        if (!authed) {
            return IntegrationStatus.AUTH_REQUIRED to
                "Connect your Google account to use Tasks."
        }
        return when (val result = tasksRepository.probeAccess(accId)) {
            is MoResult.Success -> IntegrationStatus.CONNECTED to
                "Google Tasks is connected."
            is MoResult.Failure -> when (val e = result.error) {
                is MoError.Authentication -> IntegrationStatus.AUTH_REQUIRED to
                    "Your Google authorization expired or was revoked. Reconnect to continue."
                is MoError.PermissionDenied -> IntegrationStatus.PERMISSION_REQUIRED to
                    "The Tasks permission isn't granted yet. Sign in again " +
                        "to grant it — your mail and calendar keep working."
                is MoError.Network -> IntegrationStatus.OFFLINE to
                    "You're offline. Google Tasks will work when you're back online."
                is MoError.RateLimited -> IntegrationStatus.ERROR to
                    "Google Tasks is rate-limiting requests right now. Try again later."
                is MoError.Api -> IntegrationStatus.ERROR to
                    "Google Tasks isn't responding as expected (${e.code ?: "unknown"})."
                else -> IntegrationStatus.ERROR to
                    "Couldn't reach Google Tasks."
            }
        }
    }

    private suspend fun resolveEmail(accountId: String?): String? {
        if (accountId.isNullOrBlank()) return null
        return try {
            when (val s = authRepository.observeAuthState(AccountId(accountId)).firstOrNull()) {
                is GoogleAuthState.Connected -> s.email
                is GoogleAuthState.AuthError -> s.email
                else -> null
            }
        } catch (t: Throwable) {
            null
        }
    }

    private companion object {
        const val TAG = "TasksIntegration"
    }
}
