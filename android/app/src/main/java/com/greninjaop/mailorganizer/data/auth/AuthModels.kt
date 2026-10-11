package com.greninjaop.mailorganizer.data.auth

import com.greninjaop.mailorganizer.core.AccountId

/**
 * Authentication state for a Google account (Phase 3).
 *
 * Represents the OAuth authorization state without exposing raw tokens.
 * Tokens are managed internally by the [AuthRepository] implementation
 * and stored in secure storage.
 */
sealed interface GoogleAuthState {
    /** No Google account connected. */
    data object Disconnected : GoogleAuthState

    /** OAuth flow in progress. */
    data object Connecting : GoogleAuthState

    /** Connected with valid (or refreshable) credentials. */
    data class Connected(
        val accountId: AccountId,
        val email: String,
        val displayName: String?,
    ) : GoogleAuthState

    /** Connected but authorization was revoked or refresh failed. */
    data class AuthError(
        val accountId: AccountId,
        val email: String,
        val reason: AuthErrorReason,
    ) : GoogleAuthState
}

/**
 * Reasons for authentication errors (Phase 3 §24, §25).
 *
 * These are user-facing categories; internal Google error details
 * are never exposed to the UI or logs.
 */
enum class AuthErrorReason {
    /** User denied the OAuth consent. */
    USER_DENIED,

    /** Authorization was revoked (via Google account settings or API). */
    REVOKED,

    /** Refresh token invalid or expired; re-authentication required. */
    REFRESH_FAILED,

    /** Network error during authentication. */
    NETWORK_ERROR,

    /** Unknown error during authentication. */
    UNKNOWN,
}

/**
 * Result of an authentication operation (Phase 3).
 */
sealed interface AuthResult {
    /** Authentication succeeded. */
    data class Success(
        val accountId: AccountId,
        val email: String,
        val displayName: String?,
    ) : AuthResult

    /** User cancelled the authentication flow. */
    data object Cancelled : AuthResult

    /** Authentication failed. */
    data class Failure(
        val reason: AuthErrorReason,
    ) : AuthResult
}
