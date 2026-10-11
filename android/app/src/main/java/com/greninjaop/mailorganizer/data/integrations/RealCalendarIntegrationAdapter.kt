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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Google Calendar integration adapter — Phase 15 (real implementation).
 *
 * Connects Mail Organizer's meeting/deadline intelligence to Google Calendar
 * via the official Calendar API.
 *
 * Core principle (Phase 15):
 * ```
 * Email → Meeting/Deadline Intelligence → Action Candidate
 *   → Calendar Action Proposal → User Review → Explicit Confirmation
 *   → Google Calendar API → Verified Result → Local Action State
 * ```
 *
 * NEVER creates events without explicit user confirmation.
 */
class RealCalendarIntegrationAdapter(
    private val authRepository: AuthRepository,
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
            scope = "https://www.googleapis.com/auth/calendar.events",
            purpose = "Add events you explicitly choose to your calendar. " +
                "Mailstack never creates events on its own.",
            required = true,
        ),
    )

    override suspend fun snapshot(accountId: String?): IntegrationSnapshot {
        val accId = accountId?.let { AccountId(it) }
        val isConnected = accId?.let { authRepository.isConnected(it) } ?: false

        return IntegrationSnapshot(
            id = id,
            displayName = displayName,
            provider = provider,
            accountId = accountId,
            accountEmail = null, // Resolved via auth state
            status = if (isConnected) IntegrationStatus.CONNECTED else IntegrationStatus.DISCONNECTED,
            statusReason = if (isConnected) "Connected" else "Connect your Google account to use Calendar",
            declaredCapabilities = declaredCapabilities,
            requiredPermissions = requiredPermissions,
            lastCheckedEpochMs = clock(),
        )
    }

    override suspend fun connect(): MoResult<Unit> {
        // Connection is handled via the AuthRepository OAuth flow (Phase 3).
        // This adapter uses the existing Google account connection.
        return MoResult.Failure(
            MoError.Integration(
                integration = id.value,
                message = "Connect via the Google account settings. Calendar uses your connected Google account.",
            ),
        )
    }

    override suspend fun disconnect(accountId: String?): MoResult<Unit> {
        // Calendar shares the Google account; disconnect via AuthRepository.
        return MoResult.Success(Unit)
    }

    override suspend fun refresh(accountId: String?): IntegrationSnapshot =
        snapshot(accountId)

    /**
     * Creates a calendar event (Phase 15).
     *
     * MUST only be called after explicit user confirmation via the
     * Action Engine's Proposal → Review → Confirmation flow.
     *
     * @param accountId The Google account to create the event in.
     * @param title Event title.
     * @param description Event description (optional).
     * @param startEpochMs Event start time (milliseconds since epoch).
     * @param endEpochMs Event end time (milliseconds since epoch).
     * @return [MoResult] with the created event ID on success.
     */
    suspend fun createEvent(
        accountId: AccountId,
        title: String,
        description: String?,
        startEpochMs: Long,
        endEpochMs: Long,
    ): MoResult<String> {
        val accessToken = authRepository.getValidAccessToken(accountId)
            ?: return MoResult.Failure(
                MoError.Integration(
                    integration = id.value,
                    message = "Not connected. Connect your Google account first.",
                ),
            )

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://www.googleapis.com/calendar/v3/calendars/primary/events")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val eventJson = JSONObject().apply {
                    put("summary", title)
                    if (description != null) put("description", description)
                    put("start", JSONObject().apply {
                        put("dateTime", formatRfc3339(startEpochMs))
                        put("timeZone", TimeZone.getDefault().id)
                    })
                    put("end", JSONObject().apply {
                        put("dateTime", formatRfc3339(endEpochMs))
                        put("timeZone", TimeZone.getDefault().id)
                    })
                }

                connection.outputStream.use { os ->
                    os.write(eventJson.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val eventId = json.optString("id", "")
                    MoLogger.i(TAG, "Calendar event created: $eventId")
                    MoResult.Success(eventId)
                } else {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(200)
                    MoLogger.w(TAG, "Calendar event creation failed: HTTP $responseCode: $error")
                    MoResult.Failure(
                        MoError.Integration(
                            integration = id.value,
                            message = "Failed to create event: HTTP $responseCode",
                        ),
                    )
                }
            } catch (e: Exception) {
                MoLogger.e(TAG, "Calendar event creation failed", e)
                MoResult.Failure(
                    MoError.Integration(
                        integration = id.value,
                        message = "Network error: ${e.message?.take(100)}",
                    ),
                )
            }
        }
    }

    private fun formatRfc3339(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(epochMs))
    }

    private companion object {
        const val TAG = "CalendarIntegration"
    }
}
