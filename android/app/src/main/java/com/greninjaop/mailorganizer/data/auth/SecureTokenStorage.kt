package com.greninjaop.mailorganizer.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.MoLogger

/**
 * Secure token storage using EncryptedSharedPreferences (Phase 3 §16).
 *
 * Tokens are NEVER stored in:
 * - Room database
 * - Plain SharedPreferences
 * - Plaintext files
 * - Logs
 *
 * This class is the ONLY place that touches raw OAuth tokens.
 * The [AuthRepository] implementation uses this; no other code
 * should access tokens directly.
 */
class SecureTokenStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            MoLogger.e(TAG, "Failed to create encrypted prefs", e)
            throw e
        }
    }

    /**
     * Stores tokens for an account. Overwrites any existing tokens.
     */
    fun storeTokens(
        accountId: AccountId,
        accessToken: String,
        refreshToken: String?,
        expiresAtEpochMs: Long,
        email: String,
        displayName: String?,
    ) {
        prefs.edit()
            .putString(key(accountId, KEY_ACCESS_TOKEN), accessToken)
            .putString(key(accountId, KEY_REFRESH_TOKEN), refreshToken)
            .putLong(key(accountId, KEY_EXPIRES_AT), expiresAtEpochMs)
            .putString(key(accountId, KEY_EMAIL), email)
            .putString(key(accountId, KEY_DISPLAY_NAME), displayName)
            .apply()
    }

    /**
     * Retrieves the access token. Returns null if not found.
     * Callers MUST NOT log or persist this value.
     */
    fun getAccessToken(accountId: AccountId): String? =
        prefs.getString(key(accountId, KEY_ACCESS_TOKEN), null)

    fun getRefreshToken(accountId: AccountId): String? =
        prefs.getString(key(accountId, KEY_REFRESH_TOKEN), null)

    fun getExpiresAt(accountId: AccountId): Long =
        prefs.getLong(key(accountId, KEY_EXPIRES_AT), 0L)

    fun getEmail(accountId: AccountId): String? =
        prefs.getString(key(accountId, KEY_EMAIL), null)

    fun getDisplayName(accountId: AccountId): String? =
        prefs.getString(key(accountId, KEY_DISPLAY_NAME), null)

    /**
     * Updates only the access token and expiry (after refresh).
     */
    fun updateAccessToken(
        accountId: AccountId,
        accessToken: String,
        expiresAtEpochMs: Long,
    ) {
        prefs.edit()
            .putString(key(accountId, KEY_ACCESS_TOKEN), accessToken)
            .putLong(key(accountId, KEY_EXPIRES_AT), expiresAtEpochMs)
            .apply()
    }

    /**
     * Clears all tokens for an account.
     */
    fun clearTokens(accountId: AccountId) {
        prefs.edit()
            .remove(key(accountId, KEY_ACCESS_TOKEN))
            .remove(key(accountId, KEY_REFRESH_TOKEN))
            .remove(key(accountId, KEY_EXPIRES_AT))
            .remove(key(accountId, KEY_EMAIL))
            .remove(key(accountId, KEY_DISPLAY_NAME))
            .apply()
    }

    /**
     * Returns true if tokens exist for the account.
     */
    fun hasTokens(accountId: AccountId): Boolean =
        prefs.contains(key(accountId, KEY_ACCESS_TOKEN))

    private fun key(accountId: AccountId, suffix: String): String =
        "auth_${accountId.value}_$suffix"

    private companion object {
        const val TAG = "SecureTokenStorage"
        const val PREFS_NAME = "mailorganizer_secure_auth"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
    }
}
