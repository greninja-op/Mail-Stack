package com.greninjaop.mailorganizer.data.integrations

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.integrations.IntegrationAdapter
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.core.integrations.PermissionDescription
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.sync.GmailSyncApi
import com.greninjaop.mailorganizer.data.sync.SyncApiException

/**
 * Gmail integration adapter (Phase 17).
 *
 * Wraps Phase 4's [GmailSyncApi] seam — the app's local sync engine — and
 * reports its status honestly:
 * - No account in scope → [IntegrationStatus.DISCONNECTED].
 * - Account exists but Google authorization is missing or unverified
 *   → [IntegrationStatus.AUTH_REQUIRED] with an explicit reason.
 * - [IntegrationStatus.CONNECTED] is only reported when the Phase 3 OAuth
 *   grant is verified; the manager must never claim it otherwise.
 *
 * Declared capabilities describe what Gmail provides once connected; they
 * are not usable until then. MODIFY_EMAIL additionally requires the
 * optional gmail.modify grant (Phase 22, incremental authorization).
 * Least-privilege: gmail.send is never requested (phase §7).
 */
class GmailIntegrationAdapter(
    private val syncApi: GmailSyncApi,
    private val accounts: AccountRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : IntegrationAdapter {

    override val id: IntegrationId = IntegrationId.GMAIL
    override val displayName: String = "Gmail"
    override val provider: String = "Google"

    override val declaredCapabilities: Set<IntegrationCapability> = setOf(
        IntegrationCapability.READ_EMAIL,
        IntegrationCapability.SYNC_EMAIL,
        // Phase 22: declared, but only usable once the account grants
        // gmail.modify through incremental authorization. The snapshot
        // never reports CONNECTED without verified OAuth, so this is never
        // advertised as usable without the grant (phase §12).
        IntegrationCapability.MODIFY_EMAIL,
    )

    override val requiredPermissions: List<PermissionDescription> = listOf(
        PermissionDescription(
            scope = "gmail.readonly",
            purpose = "Read and sync your Gmail messages. Mail Organizer " +
                "never modifies or sends mail with this permission.",
            required = true,
        ),
        // Phase 22: optional, requested only when the user first attempts a
        // write action — never bundled into sign-in (phase §7, §9, §10).
        PermissionDescription(
            scope = "gmail.modify",
            purpose = "Mark messages read/unread, star, archive, move to " +
                "trash, and change labels — only when you explicitly confirm " +
                "each action. Mail Organizer never sends mail or deletes " +
                "permanently with this permission.",
            required = false,
        ),
    )

    override suspend fun snapshot(accountId: String?): IntegrationSnapshot {
        val account = accountId?.let { accounts.getById(it) }
        val (status, reason) = when {
            account == null -> IntegrationStatus.DISCONNECTED to
                "No account added yet."
            account.googleAccountId.isNullOrBlank() -> IntegrationStatus.AUTH_REQUIRED to
                "Gmail isn't connected yet — connect your Google account " +
                    "to enable sync. Your mail stays on your device until then."
            else -> probeSyncApi(accountId)
        }
        return IntegrationSnapshot(
            id = id,
            displayName = displayName,
            provider = provider,
            accountId = accountId,
            accountEmail = account?.emailAddress,
            status = status,
            statusReason = reason,
            declaredCapabilities = declaredCapabilities,
            requiredPermissions = requiredPermissions,
            lastCheckedEpochMs = clock(),
        )
    }

    override suspend fun connect(): MoResult<Unit> =
        MoResult.Failure(
            MoError.InvalidConfiguration(
                "Google sign-in is handled in the Accounts screen, not here. " +
                    "Nothing was connected.",
            ),
        )

    /**
     * Idempotent safe disconnect (phase §18): with no live connection
     * there is nothing to revoke; succeeds without touching mail,
     * classifications, rules, or action history.
     */
    override suspend fun disconnect(accountId: String?): MoResult<Unit> =
        MoResult.Success(Unit)

    override suspend fun refresh(accountId: String?): IntegrationSnapshot =
        snapshot(accountId)

    /**
     * Probes the sync seam to distinguish a configured client from the
     * fail-closed deferred stand-in. Any failure keeps the honest
     * AUTH_REQUIRED state — a probe must never promote to CONNECTED.
     */
    private suspend fun probeSyncApi(accountId: String): Pair<IntegrationStatus, String> {
        return try {
            syncApi.latestHistoryCursor(com.greninjaop.mailorganizer.core.AccountId(accountId))
            // A real cursor means a configured client — but without OAuth
            // verification this path is unreachable; kept as a safeguard.
            IntegrationStatus.AUTH_REQUIRED to
                "Gmail authorization needs verification — reconnect your Google account."
        } catch (e: SyncApiException.NotConfigured) {
            IntegrationStatus.AUTH_REQUIRED to
                "Gmail isn't connected yet — connect your Google account " +
                    "to enable sync. Your mail stays on your device until then."
        } catch (e: Exception) {
            IntegrationStatus.ERROR to
                "Couldn't check Gmail status: ${e.message ?: "unknown error"}"
        }
    }
}
