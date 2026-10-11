package com.greninjaop.mailorganizer.data.integrations

import android.content.Intent
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
import com.greninjaop.mailorganizer.data.auth.GoogleAuthRepository
import com.greninjaop.mailorganizer.data.calendar.CalendarApiError
import com.greninjaop.mailorganizer.data.calendar.CalendarApiResult
import com.greninjaop.mailorganizer.data.calendar.CalendarEvent
import com.greninjaop.mailorganizer.data.calendar.CalendarInfo
import com.greninjaop.mailorganizer.data.calendar.GoogleCalendarApi
import com.greninjaop.mailorganizer.domain.calendar.CalendarEventProposal
import java.util.concurrent.ConcurrentHashMap

/**
 * Google Calendar integration adapter — Phase 15 (real implementation).
 *
 * Connects Mailstack's meeting/deadline intelligence to Google Calendar
 * via the official Calendar API v3.
 *
 * Core principle (Phase 15):
 * ```
 * Email → Meeting/Deadline Intelligence → Action Candidate
 *   → Calendar Action Proposal → User Review → Explicit Confirmation
 *   → Google Calendar API → Verified Result → Local Action State
 * ```
 *
 * NEVER creates events without explicit user confirmation — execution
 * goes through [com.greninjaop.mailorganizer.domain.calendar.CalendarActionExecutor],
 * which the Action Engine only invokes after the confirmation boundary.
 *
 * Honesty rules:
 * - CONNECTED is reported only after [GoogleCalendarApi.probeAccess]
 *   proves the token works AND the calendar scope is granted (§38).
 * - Account isolation: every call carries the [AccountId]; tokens are
 *   fetched per-account via [AuthRepository.getValidAccessToken] and
 *   never mixed (§9, §10, §36).
 * - Duplicate safety (§27–§29): deterministic idempotency keys +
 *   server-side extended-property lookup + a process-local receipt
 *   cache. A retry with the same key can never create a second event.
 */
class RealCalendarIntegrationAdapter(
    private val authRepository: AuthRepository,
    private val api: GoogleCalendarApi = GoogleCalendarApi(),
    private val clock: () -> Long = System::currentTimeMillis,
) : IntegrationAdapter {

    override val id: IntegrationId = IntegrationId.CALENDAR
    override val displayName: String = "Google Calendar"
    override val provider: String = "Google"

    override val declaredCapabilities: Set<IntegrationCapability> = setOf(
        IntegrationCapability.READ_CALENDAR_METADATA,
        IntegrationCapability.CREATE_EVENT,
    )

    override val requiredPermissions: List<PermissionDescription> = listOf(
        PermissionDescription(
            scope = GoogleAuthRepository.SCOPE_CALENDAR_EVENTS,
            purpose = "Read your calendar list and add events you explicitly choose. " +
                "Mailstack never creates events on its own.",
            required = true,
        ),
    )

    /**
     * Process-local execution receipts: idempotency key → created event
     * id. Optimization only — the server-side extended-property lookup
     * is the cross-process safety net, so a process restart can never
     * cause duplicates.
     */
    private val receiptCache = ConcurrentHashMap<String, String>()

    private fun receiptKey(accountId: AccountId, calendarId: String, idempotencyKey: String) =
        "${accountId.value}|$calendarId|$idempotencyKey"

    // ---------- IntegrationAdapter contract ----------

    override suspend fun snapshot(accountId: String?): IntegrationSnapshot {
        return try {
            buildSnapshot(accountId)
        } catch (t: Throwable) {
            // Total: never throws (contract).
            MoLogger.e(TAG, "Calendar snapshot failed: ${t.javaClass.simpleName}")
            IntegrationSnapshot(
                id = id,
                displayName = displayName,
                provider = provider,
                accountId = accountId,
                accountEmail = null,
                status = IntegrationStatus.ERROR,
                statusReason = "Couldn't check Calendar status.",
                declaredCapabilities = declaredCapabilities,
                requiredPermissions = requiredPermissions,
                lastCheckedEpochMs = clock(),
            )
        }
    }

    private suspend fun buildSnapshot(accountId: String?): IntegrationSnapshot {
        if (accountId.isNullOrBlank()) {
            return snapshotFor(accountId, IntegrationStatus.DISCONNECTED, "No account selected.")
        }
        val accId = AccountId(accountId)

        if (!authRepository.isConnected(accId)) {
            return snapshotFor(
                accountId, IntegrationStatus.DISCONNECTED,
                "Connect your Google account to use Calendar.",
            )
        }
        val token = authRepository.getValidAccessToken(accId)
            ?: return snapshotFor(
                accountId, IntegrationStatus.AUTH_REQUIRED,
                "Your Google sign-in expired. Reconnect your account.",
            )

        return when (val probe = api.probeAccess(token)) {
            is CalendarApiResult.Success -> snapshotFor(
                accountId, IntegrationStatus.CONNECTED, "Calendar access verified.",
            )
            is CalendarApiResult.Failure -> when (probe.error) {
                CalendarApiError.AUTH -> snapshotFor(
                    accountId, IntegrationStatus.AUTH_REQUIRED,
                    "Your Google sign-in expired or was revoked. Reconnect your account.",
                )
                CalendarApiError.INSUFFICIENT_SCOPE -> snapshotFor(
                    accountId, IntegrationStatus.PERMISSION_REQUIRED,
                    "Calendar permission needed. Grant access to continue.",
                )
                CalendarApiError.NETWORK -> snapshotFor(
                    accountId, IntegrationStatus.OFFLINE,
                    "You're offline. Calendar will work when you're back online.",
                )
                else -> snapshotFor(
                    accountId, IntegrationStatus.ERROR,
                    "Calendar isn't reachable right now.",
                )
            }
        }
    }

    private fun snapshotFor(
        accountId: String?,
        status: IntegrationStatus,
        reason: String,
    ) = IntegrationSnapshot(
        id = id,
        displayName = displayName,
        provider = provider,
        accountId = accountId,
        accountEmail = null,
        status = status,
        statusReason = reason,
        declaredCapabilities = declaredCapabilities,
        requiredPermissions = requiredPermissions,
        lastCheckedEpochMs = clock(),
    )

    override suspend fun connect(): MoResult<Unit> {
        // Calendar shares the Google account; granting the calendar scope
        // is an incremental OAuth step launched from the UI via
        // [incrementalAuthIntent]. This adapter cannot launch UI itself,
        // so connect() honestly reports that instead of faking success.
        return MoResult.Failure(
            MoError.Integration(
                integration = id.value,
                message = "Calendar connects through your Google account. " +
                    "Grant the Calendar permission to finish setup.",
            ),
        )
    }

    /**
     * Incremental-authorization path (Phase 15 §6, §8): the UI launches
     * this intent to add ONLY the calendar scope to an already-connected
     * account. Never requested during the initial Phase 3 sign-in.
     */
    fun incrementalAuthIntent(accountId: AccountId): Intent =
        authRepository.createIncrementalAuthIntent(
            accountId,
            listOf(GoogleAuthRepository.SCOPE_CALENDAR_EVENTS),
        )

    override suspend fun disconnect(accountId: String?): MoResult<Unit> {
        // Idempotent; clears only this integration's process-local
        // receipts for the account (§35). Never deletes Gmail data, local
        // mail, other accounts, or Calendar events. Revoking the Google
        // grant itself is owned by the account-level AuthRepository
        // disconnect — this adapter must not silently revoke it.
        if (!accountId.isNullOrBlank()) {
            val prefix = "$accountId|"
            receiptCache.keys.removeIf { it.startsWith(prefix) }
        }
        return MoResult.Success(Unit)
    }

    override suspend fun refresh(accountId: String?): IntegrationSnapshot =
        snapshot(accountId)

    // ---------- Calendar operations (Phase 15 §11–§14) ----------

    /**
     * Writable calendars for the account, primary first. Used for
     * calendar selection (§37) and default-target resolution.
     */
    suspend fun listWritableCalendars(accountId: AccountId): MoResult<List<CalendarInfo>> {
        val token = authRepository.getValidAccessToken(accountId)
            ?: return MoResult.Failure(MoError.Authentication("Connect your Google account first."))
        return when (val r = api.listCalendars(token)) {
            is CalendarApiResult.Success ->
                MoResult.Success(r.value.filter { it.isWritable }.sortedByDescending { it.primary })
            is CalendarApiResult.Failure -> MoResult.Failure(r.toMoError())
        }
    }

    suspend fun listUpcomingEvents(
        accountId: AccountId,
        calendarId: String,
        timeMinEpochMs: Long,
        maxResults: Int = 50,
    ): MoResult<List<CalendarEvent>> {
        val token = authRepository.getValidAccessToken(accountId)
            ?: return MoResult.Failure(MoError.Authentication("Connect your Google account first."))
        return when (val r = api.listUpcomingEvents(token, calendarId, timeMinEpochMs, maxResults)) {
            is CalendarApiResult.Success -> MoResult.Success(r.value)
            is CalendarApiResult.Failure -> MoResult.Failure(r.toMoError())
        }
    }

    /**
     * Idempotent event creation (Phase 15 §27–§29).
     *
     * MUST only be called after explicit user confirmation (enforced by
     * the Action Engine's confirmation boundary, not here).
     *
     * Duplicate-safety order:
     * 1. Process-local receipt cache → verify the event still exists.
     * 2. Server lookup by idempotency key → reuse the existing event.
     * 3. Insert with client-generated iCalUID + idempotency key.
     * 4. On network failure during insert → recovery lookup before
     *    reporting failure, so a retry reuses instead of duplicating.
     */
    suspend fun createEvent(
        accountId: AccountId,
        proposal: CalendarEventProposal,
    ): MoResult<CalendarEvent> {
        if (proposal.accountId != accountId.value) {
            // Defense in depth: the proposal is bound to one account (§36).
            return MoResult.Failure(
                MoError.Integration(
                    integration = id.value,
                    message = "Account mismatch: this action belongs to a different account.",
                ),
            )
        }
        val token = authRepository.getValidAccessToken(accountId)
            ?: return MoResult.Failure(MoError.Authentication("Connect your Google account first."))
        val calendarId = proposal.calendarId
        if (calendarId.isBlank()) {
            return MoResult.Failure(
                MoError.InvalidConfiguration("No target calendar was chosen."),
            )
        }
        val key = receiptKey(accountId, calendarId, proposal.idempotencyKey)

        // 1. Receipt cache: verify the recorded event still exists.
        receiptCache[key]?.let { eventId ->
            when (val v = api.getEvent(token, calendarId, eventId)) {
                is CalendarApiResult.Success -> {
                    MoLogger.i(TAG, "Reusing cached event $eventId (idempotent retry)")
                    return MoResult.Success(v.value)
                }
                is CalendarApiResult.Failure -> {
                    // Stale receipt — drop it and continue to server lookup.
                    receiptCache.remove(key, eventId)
                    if (v.error == CalendarApiError.AUTH ||
                        v.error == CalendarApiError.INSUFFICIENT_SCOPE
                    ) {
                        return MoResult.Failure(v.toMoError())
                    }
                }
            }
        }

        // 2. Server lookup: catches events created by an earlier attempt
        // whose response was lost (including previous process runs).
        when (val lookup = api.findEventByIdempotencyKey(token, calendarId, proposal.idempotencyKey)) {
            is CalendarApiResult.Success -> {
                val existing = lookup.value
                if (existing != null) {
                    receiptCache[key] = existing.id
                    MoLogger.i(TAG, "Found existing event ${existing.id} for idempotency key")
                    return MoResult.Success(existing)
                }
            }
            is CalendarApiResult.Failure -> {
                // Auth/scope problems are terminal; network failure here
                // means we must NOT blindly insert (would risk a dup).
                return MoResult.Failure(lookup.toMoError())
            }
        }

        // 3. Insert.
        return when (val created = api.createEvent(token, proposal.toEventWrite())) {
            is CalendarApiResult.Success -> {
                receiptCache[key] = created.value.id
                MoLogger.i(TAG, "Calendar event created: ${created.value.id}")
                MoResult.Success(created.value)
            }
            is CalendarApiResult.Failure -> {
                if (created.error == CalendarApiError.NETWORK) {
                    // 4. Unknown outcome: the event may exist server-side.
                    // Recover via lookup instead of reporting a blind failure.
                    return recoverAfterNetworkFailure(token, calendarId, proposal, key)
                }
                if (created.error == CalendarApiError.CONFLICT) {
                    // Someone else holds this identity — look it up.
                    when (val l = api.findEventByIdempotencyKey(
                        token, calendarId, proposal.idempotencyKey,
                    )) {
                        is CalendarApiResult.Success -> {
                            val existing = l.value
                            if (existing != null) {
                                receiptCache[key] = existing.id
                                return MoResult.Success(existing)
                            }
                        }
                        is CalendarApiResult.Failure -> {
                            MoLogger.w(TAG, "Conflict recovery lookup failed")
                        }
                    }
                }
                MoResult.Failure(created.toMoError())
            }
        }
    }

    private suspend fun recoverAfterNetworkFailure(
        token: String,
        calendarId: String,
        proposal: CalendarEventProposal,
        key: String,
    ): MoResult<CalendarEvent> {
        return when (val l = api.findEventByIdempotencyKey(token, calendarId, proposal.idempotencyKey)) {
            is CalendarApiResult.Success -> {
                val existing = l.value
                if (existing != null) {
                    receiptCache[key] = existing.id
                    MoLogger.i(
                        TAG,
                        "Recovered event ${existing.id} after network failure (no duplicate created)",
                    )
                    MoResult.Success(existing)
                } else {
                    // Definitely not created — safe for the user to retry
                    // with the same idempotency key.
                    MoResult.Failure(
                        MoError.Network(
                            "Couldn't reach Google Calendar, and the event wasn't created. " +
                                "It's safe to try again.",
                        ),
                    )
                }
            }
            is CalendarApiResult.Failure ->
                MoResult.Failure(l.toMoError())
        }
    }

    suspend fun updateEvent(
        accountId: AccountId,
        proposal: CalendarEventProposal,
        eventId: String,
    ): MoResult<CalendarEvent> {
        val token = authRepository.getValidAccessToken(accountId)
            ?: return MoResult.Failure(MoError.Authentication("Connect your Google account first."))
        return when (val r = api.updateEvent(token, proposal.toEventWrite(), eventId)) {
            is CalendarApiResult.Success -> MoResult.Success(r.value)
            is CalendarApiResult.Failure -> MoResult.Failure(r.toMoError())
        }
    }

    suspend fun deleteEvent(
        accountId: AccountId,
        calendarId: String,
        eventId: String,
    ): MoResult<Unit> {
        val token = authRepository.getValidAccessToken(accountId)
            ?: return MoResult.Failure(MoError.Authentication("Connect your Google account first."))
        return when (val r = api.deleteEvent(token, calendarId, eventId)) {
            is CalendarApiResult.Success -> {
                receiptCache.entries.removeIf {
                    it.key.startsWith("${accountId.value}|$calendarId|") && it.value == eventId
                }
                MoResult.Success(Unit)
            }
            is CalendarApiResult.Failure -> MoResult.Failure(r.toMoError())
        }
    }

    private companion object {
        const val TAG = "CalendarIntegration"
    }
}
