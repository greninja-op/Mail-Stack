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
     * sign-in flow for a new account. The caller starts this intent;
     * Google redirects back to the app and the Activity forwards the
     * redirect via [handleOAuthResult].
     *
     * @param additionalScopes Scopes beyond the Phase 3 base set. Used by
     * later phases (Calendar, Tasks, Gmail write) for incremental
     * authorization — never requested all-at-once up front.
     */
    fun createOAuthIntent(
        additionalScopes: List<String> = emptyList(),
    ): android.content.Intent

    /**
     * Creates an OAuth [android.content.Intent] that adds [additionalScopes]
     * to an already-connected account (incremental authorization,
     * `include_granted_scopes=true`). The redirect is handled like a normal
     * connection; tokens are updated in place for the same account.
     */
    fun createIncrementalAuthIntent(
        accountId: AccountId,
        additionalScopes: List<String>,
    ): android.content.Intent

    /**
     * Emits the result of each OAuth redirect handled by
     * [handleOAuthResult]. The Connect UI collects this to drive its
     * connecting/success/error states.
     */
    fun observeOAuthResults(): Flow<AuthResult>

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
