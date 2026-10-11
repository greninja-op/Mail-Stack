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
 * Google Tasks integration adapter — Phase 16 (real implementation).
 *
 * Connects Mail Organizer's action intelligence to Google Tasks
 * via the official Tasks API.
 *
 * Core principle (Phase 16):
 * ```
 * Email → Action Intelligence → Task Proposal
 *   → User Review → Explicit Confirmation
 *   → Google Tasks API → Verified Result
 * ```
 *
 * NEVER creates tasks without explicit user confirmation.
 */
class RealTasksIntegrationAdapter(
    private val authRepository: AuthRepository,
    private val clock: () -> Long = System::currentTimeMillis,
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

    override suspend fun snapshot(accountId: String?): IntegrationSnapshot {
        val accId = accountId?.let { AccountId(it) }
        val isConnected = accId?.let { authRepository.isConnected(it) } ?: false

        return IntegrationSnapshot(
            id = id,
            displayName = displayName,
            provider = provider,
            accountId = accountId,
            accountEmail = null,
            status = if (isConnected) IntegrationStatus.CONNECTED else IntegrationStatus.DISCONNECTED,
            statusReason = if (isConnected) "Connected" else "Connect your Google account to use Tasks",
            declaredCapabilities = declaredCapabilities,
            requiredPermissions = requiredPermissions,
            lastCheckedEpochMs = clock(),
        )
    }

    override suspend fun connect(): MoResult<Unit> {
        return MoResult.Failure(
            MoError.Integration(
                integration = id.value,
                message = "Connect via the Google account settings. Tasks uses your connected Google account.",
            ),
        )
    }

    override suspend fun disconnect(accountId: String?): MoResult<Unit> =
        MoResult.Success(Unit)

    override suspend fun refresh(accountId: String?): IntegrationSnapshot =
        snapshot(accountId)

    /**
     * Creates a task (Phase 16).
     *
     * MUST only be called after explicit user confirmation.
     *
     * @param accountId The Google account to create the task in.
     * @param title Task title.
     * @param notes Task notes (optional).
     * @param dueEpochMs Due date (milliseconds since epoch, optional).
     * @return [MoResult] with the created task ID on success.
     */
    suspend fun createTask(
        accountId: AccountId,
        title: String,
        notes: String?,
        dueEpochMs: Long?,
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
                val url = URL("https://tasks.googleapis.com/tasks/v1/lists/@default/tasks")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val taskJson = JSONObject().apply {
                    put("title", title)
                    if (notes != null) put("notes", notes)
                    if (dueEpochMs != null) put("due", formatRfc3339(dueEpochMs))
                }

                connection.outputStream.use { os ->
                    os.write(taskJson.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val taskId = json.optString("id", "")
                    MoLogger.i(TAG, "Task created: $taskId")
                    MoResult.Success(taskId)
                } else {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(200)
                    MoLogger.w(TAG, "Task creation failed: HTTP $responseCode: $error")
                    MoResult.Failure(
                        MoError.Integration(
                            integration = id.value,
                            message = "Failed to create task: HTTP $responseCode",
                        ),
                    )
                }
            } catch (e: Exception) {
                MoLogger.e(TAG, "Task creation failed", e)
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
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }

    private companion object {
        const val TAG = "TasksIntegration"
    }
}
