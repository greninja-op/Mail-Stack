package com.greninjaop.mailorganizer.data.auth

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.MoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gmail API client (Phase 3 §26).
 *
 * Uses direct HTTPS calls to the Gmail REST API (no heavy Google client
 * libraries). The client is fail-closed: if no valid token is available,
 * operations return an honest "not connected" state rather than crashing.
 *
 * Phase 3 scope: Gmail API client foundation + authenticated smoke test.
 * Mailbox synchronization is Phase 4.
 */
class GmailApiClient(
    private val authRepository: AuthRepository,
) {
    /**
     * Performs an authenticated smoke test: fetches the Gmail profile.
     * Returns the email address on success, null on failure.
     *
     * This verifies that the OAuth grant works without syncing any mail.
     */
    suspend fun smokeTest(accountId: AccountId): SmokeTestResult {
        val accessToken = authRepository.getValidAccessToken(accountId)
            ?: return SmokeTestResult.Failure("Not connected")

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://gmail.googleapis.com/gmail/v1/users/me/profile")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val email = json.optString("emailAddress", "unknown")
                    MoLogger.i(TAG, "Gmail smoke test succeeded for $email")
                    SmokeTestResult.Success(email)
                } else if (responseCode == 401) {
                    MoLogger.w(TAG, "Gmail smoke test: unauthorized (token invalid)")
                    SmokeTestResult.Failure("Unauthorized: token invalid or expired")
                } else if (responseCode == 403) {
                    MoLogger.w(TAG, "Gmail smoke test: forbidden")
                    SmokeTestResult.Failure("Forbidden: insufficient permissions")
                } else {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(200)
                    MoLogger.w(TAG, "Gmail smoke test failed: HTTP $responseCode: $error")
                    SmokeTestResult.Failure("API error: HTTP $responseCode")
                }
            } catch (e: Exception) {
                MoLogger.e(TAG, "Gmail smoke test failed", e)
                SmokeTestResult.Failure("Network error: ${e.message?.take(100)}")
            }
        }
    }

    sealed interface SmokeTestResult {
        data class Success(val email: String) : SmokeTestResult
        data class Failure(val reason: String) : SmokeTestResult
    }

    /**
     * Marks a message as read/unread (Phase 22).
     *
     * Requires gmail.modify scope. MUST only be called after explicit
     * user action (never automatic).
     */
    suspend fun markAsRead(
        accountId: AccountId,
        messageId: String,
        isRead: Boolean,
    ): MoResult<Unit> = modifyLabels(
        accountId = accountId,
        messageId = messageId,
        addLabels = if (isRead) emptyList() else listOf("UNREAD"),
        removeLabels = if (isRead) listOf("UNREAD") else emptyList(),
    )

    /**
     * Archives a message (removes INBOX label) (Phase 22).
     *
     * MUST only be called after explicit user confirmation.
     */
    suspend fun archiveMessage(
        accountId: AccountId,
        messageId: String,
    ): MoResult<Unit> = modifyLabels(
        accountId = accountId,
        messageId = messageId,
        addLabels = emptyList(),
        removeLabels = listOf("INBOX"),
    )

    /**
     * Modifies message labels via Gmail API (Phase 22).
     */
    private suspend fun modifyLabels(
        accountId: AccountId,
        messageId: String,
        addLabels: List<String>,
        removeLabels: List<String>,
    ): MoResult<Unit> {
        // Note: Write operations require gmail.modify scope.
        // Phase 3 requested gmail.readonly only. The OAuth flow must
        // request additional scopes for write features.
        val accessToken = authRepository.getValidAccessToken(accountId)
            ?: return MoResult.Failure(
                com.greninjaop.mailorganizer.MoError.Integration(
                    integration = "gmail",
                    message = "Not connected. Connect your Google account first.",
                ),
            )

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId/modify")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val body = JSONObject().apply {
                    put("addLabelIds", org.json.JSONArray(addLabels))
                    put("removeLabelIds", org.json.JSONArray(removeLabels))
                }

                connection.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    MoLogger.i(TAG, "Message $messageId labels modified")
                    MoResult.Success(Unit)
                } else {
                    MoLogger.w(TAG, "Label modification failed: HTTP $responseCode")
                    MoResult.Failure(
                        com.greninjaop.mailorganizer.MoError.Integration(
                            integration = "gmail",
                            message = "Failed to modify message: HTTP $responseCode",
                        ),
                    )
                }
            } catch (e: Exception) {
                MoLogger.e(TAG, "Label modification failed", e)
                MoResult.Failure(
                    com.greninjaop.mailorganizer.MoError.Integration(
                        integration = "gmail",
                        message = "Network error: ${e.message?.take(100)}",
                    ),
                )
            }
        }
    }

    private companion object {
        const val TAG = "GmailApiClient"
    }
}
