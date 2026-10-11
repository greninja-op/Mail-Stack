package com.greninjaop.mailorganizer.data.auth

import com.greninjaop.mailorganizer.core.AccountId
import kotlinx.coroutines.flow.Flow

/**
 * Authentication repository abstraction (Phase 3 §17).
 *
 * Owns the Google OAuth lifecycle:
 * - Initiating the OAuth flow
 * - Secure token storage (never exposes raw tokens)
 * - Token refresh
 * - Account disconnection
 * - Auth state observation
 *
 * The Gmail API client depends on this abstraction, never on
 * token storage directly.
 */
interface AuthRepository {
    /**
     * Observes the authentication state for all connected accounts.
     */
    fun observeAuthStates(): Flow<Map<AccountId, GoogleAuthState>>

    /**
     * Observes the authentication state for a specific account.
     */
    fun observeAuthState(accountId: AccountId): Flow<GoogleAuthState>

    /**
     * Returns a valid access token for the given account, refreshing
     * if necessary. Returns null if the account is not connected or
     * refresh failed.
     *
     * This is the ONLY method that touches raw tokens. Callers must
     * never log or persist the returned token.
     */
    suspend fun getValidAccessToken(accountId: AccountId): String?

    /**
     * Initiates the Google OAuth flow for a new account connection.
     * The UI layer handles the Activity result; this method processes
     * the result.
     *
     * @return [AuthResult] indicating success, cancellation, or failure.
     */
    suspend fun handleOAuthResult(data: android.content.Intent?): AuthResult

    /**
     * Creates an OAuth [android.content.Intent] to start the Google
     * sign-in flow. The caller starts this intent with
     * startActivityForResult.
     */
    fun createOAuthIntent(): android.content.Intent

    /**
     * Disconnects the given account: revokes the token with Google,
     * clears secure storage, and updates state.
     */
    suspend fun disconnect(accountId: AccountId)

    /**
     * Checks if the given account has valid (or refreshable) credentials.
     */
    suspend fun isConnected(accountId: AccountId): Boolean
}
