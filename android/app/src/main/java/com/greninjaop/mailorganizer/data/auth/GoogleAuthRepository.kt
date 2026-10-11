package com.greninjaop.mailorganizer.data.auth

import android.content.Intent
import android.net.Uri
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Google OAuth repository implementation (Phase 3).
 *
 * Uses the OAuth2 authorization-code flow with PKCE (RFC 7636) via the
 * system browser — the Google-supported mechanism for installed apps
 * (no embedded webviews, no password collection, no scraping).
 *
 * Flow:
 * ```
 * createOAuthIntent()  →  system browser, Google consent
 *       ↓
 * redirect to com.greninjaop.mailorganizer://oauth2redirect
 *       ↓ (MainActivity.onNewIntent)
 * handleOAuthResult()  →  state check → code exchange (HTTPS POST)
 *       →  Gmail profile smoke test  →  tokens stored encrypted
 * ```
 *
 * Phase 3 requests the least-privilege base scopes only:
 * - https://www.googleapis.com/auth/gmail.readonly
 * - openid, email, profile
 *
 * Write/integration scopes (gmail.modify, calendar, tasks) are requested
 * incrementally by their own phases via [createIncrementalAuthIntent].
 *
 * Tokens live ONLY in [SecureTokenStorage] (EncryptedSharedPreferences).
 * Raw tokens are never logged, never stored in Room, never exposed to UI.
 */
class GoogleAuthRepository(
    private val tokenStorage: SecureTokenStorage,
    private val accountRepository: AccountRepository,
    private val oauthClientId: String,
    private val oauthHttp: GoogleOAuthHttp = GoogleOAuthHttp(),
    private val gmailApiBase: String = "https://gmail.googleapis.com",
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _authStates = MutableStateFlow<Map<AccountId, GoogleAuthState>>(emptyMap())
    private val authStatesFlow = _authStates.asStateFlow()

    private val _oauthResults =
        MutableSharedFlow<AuthResult>(extraBufferCapacity = 4)
    private val oauthResultsFlow = _oauthResults.asSharedFlow()

    init {
        // Restore auth state after process restart (Phase 3 §18): any
        // account with stored tokens is Connected (token validity is
        // checked lazily by getValidAccessToken, which refreshes).
        scope.launch(Dispatchers.IO) {
            try {
                val restored = mutableMapOf<AccountId, GoogleAuthState>()
                for (id in tokenStorage.getAllAccountIds()) {
                    val accountId = AccountId(id)
                    if (!tokenStorage.hasTokens(accountId)) continue
                    val email = tokenStorage.getEmail(accountId) ?: continue
                    restored[accountId] = GoogleAuthState.Connected(
                        accountId = accountId,
                        email = email,
                        displayName = tokenStorage.getDisplayName(accountId),
                    )
                }
                if (restored.isNotEmpty()) {
                    _authStates.value = restored
                    MoLogger.i(TAG, "Restored ${restored.size} connected account(s)")
                }
                // A redirect that arrived while we were dead left a pending
                // flow behind; it can no longer be completed (no intent).
                // Keep it — if the browser is still open the user can finish
                // and the redirect will arrive via onNewIntent.
            } catch (e: Exception) {
                MoLogger.e(TAG, "Auth state restore failed", e)
            }
        }
    }

    override fun observeAuthStates(): Flow<Map<AccountId, GoogleAuthState>> = authStatesFlow

    override fun observeAuthState(accountId: AccountId): Flow<GoogleAuthState> =
        authStatesFlow.map { it[accountId] ?: GoogleAuthState.Disconnected }

    override fun observeOAuthResults(): Flow<AuthResult> = oauthResultsFlow

    override suspend fun getValidAccessToken(accountId: AccountId): String? {
        val accessToken = tokenStorage.getAccessToken(accountId) ?: return null
        val expiresAt = tokenStorage.getExpiresAt(accountId)
        // Refresh proactively when < 5 minutes remain.
        if (System.currentTimeMillis() > expiresAt - REFRESH_BUFFER_MS) {
            return refreshAccessToken(accountId)
        }
        return accessToken
    }

    override fun createOAuthIntent(additionalScopes: List<String>): Intent {
        return buildAuthIntent(
            scopes = BASE_SCOPES + additionalScopes,
            upgradeAccountId = null,
        )
    }

    override fun createIncrementalAuthIntent(
        accountId: AccountId,
        additionalScopes: List<String>,
    ): Intent {
        require(additionalScopes.isNotEmpty()) { "additionalScopes must not be empty" }
        return buildAuthIntent(
            scopes = BASE_SCOPES + additionalScopes,
            upgradeAccountId = accountId,
        )
    }

    private fun buildAuthIntent(
        scopes: List<String>,
        upgradeAccountId: AccountId?,
    ): Intent {
        val verifier = PkceUtil.generateCodeVerifier()
        val state = PkceUtil.generateState()
        tokenStorage.savePendingFlow(
            PendingOAuthFlow(
                state = state,
                codeVerifier = verifier,
                scopes = scopes.distinct(),
                upgradeAccountId = upgradeAccountId,
            ),
        )
        val authUrl = Uri.Builder()
            .scheme("https")
            .authority("accounts.google.com")
            .path("/o/oauth2/v2/auth")
            .appendQueryParameter("client_id", oauthClientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopes.distinct().joinToString(" "))
            .appendQueryParameter("code_challenge", PkceUtil.codeChallenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .appendQueryParameter("access_type", "offline")
            // Incremental auth: keep already-granted scopes so upgrades
            // don't force re-consent for everything.
            .appendQueryParameter("include_granted_scopes", "true")
            .appendQueryParameter("prompt", "consent")
            .build()
            .toString()
        // Never log the URL: it carries the PKCE challenge/state.
        MoLogger.i(TAG, "OAuth authorization started (scopes=${scopes.size})")
        return Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    override suspend fun handleOAuthResult(data: Intent?): AuthResult {
        val result = processOAuthRedirect(data)
        _oauthResults.emit(result)
        return result
    }

    private suspend fun processOAuthRedirect(data: Intent?): AuthResult {
        val uri = data?.data
        if (uri == null) {
            MoLogger.w(TAG, "OAuth redirect had no data")
            return AuthResult.Failure(AuthErrorReason.UNKNOWN)
        }
        if (uri.scheme != REDIRECT_SCHEME) {
            MoLogger.w(TAG, "Ignoring non-OAuth intent")
            return AuthResult.Failure(AuthErrorReason.UNKNOWN)
        }

        val pending = tokenStorage.getPendingFlow()
        if (pending == null) {
            MoLogger.w(TAG, "OAuth redirect with no pending flow")
            return AuthResult.Failure(AuthErrorReason.UNKNOWN)
        }

        // Google-level error (user denied, etc.).
        val error = uri.getQueryParameter("error")
        if (error != null) {
            tokenStorage.clearPendingFlow()
            MoLogger.i(TAG, "OAuth consent ended with error")
            return when (error) {
                "access_denied" -> AuthResult.Failure(AuthErrorReason.USER_DENIED)
                else -> AuthResult.Failure(AuthErrorReason.UNKNOWN)
            }
        }

        // CSRF protection: the echoed state MUST match.
        val returnedState = uri.getQueryParameter("state")
        if (returnedState != pending.state) {
            tokenStorage.clearPendingFlow()
            MoLogger.w(TAG, "OAuth state mismatch — possible CSRF; rejecting")
            return AuthResult.Failure(AuthErrorReason.UNKNOWN)
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            tokenStorage.clearPendingFlow()
            MoLogger.w(TAG, "OAuth redirect missing authorization code")
            return AuthResult.Failure(AuthErrorReason.UNKNOWN)
        }

        return when (
            val tokenResult = oauthHttp.exchangeCode(
                code = code,
                codeVerifier = pending.codeVerifier,
                clientId = oauthClientId,
                redirectUri = REDIRECT_URI,
            )
        ) {
            is OAuthTokenResult.Failure -> {
                tokenStorage.clearPendingFlow()
                MoLogger.w(TAG, "Token exchange failed")
                AuthResult.Failure(mapOAuthError(tokenResult.error))
            }
            is OAuthTokenResult.Success -> {
                finishConnection(pending, tokenResult)
            }
        }
    }

    /**
     * Completes sign-in after a successful token exchange: proves the
     * grant with a minimal Gmail profile read (Phase 3 §27 smoke test —
     * no mailbox sync), derives a stable account id, stores tokens and
     * upserts the Phase 2 account record.
     */
    private suspend fun finishConnection(
        pending: PendingOAuthFlow,
        tokenResult: OAuthTokenResult.Success,
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val profile = fetchGmailProfile(tokenResult.accessToken)
            if (profile == null) {
                tokenStorage.clearPendingFlow()
                MoLogger.w(TAG, "Gmail profile smoke test failed after token exchange")
                return@withContext AuthResult.Failure(AuthErrorReason.NETWORK_ERROR)
            }

            val accountId = pending.upgradeAccountId ?: deriveAccountId(profile.email)
            val previousRefresh = tokenStorage.getRefreshToken(accountId)
            tokenStorage.storeTokens(
                accountId = accountId,
                accessToken = tokenResult.accessToken,
                // Refresh responses may omit refresh_token; keep the old one.
                refreshToken = tokenResult.refreshToken ?: previousRefresh,
                expiresAtEpochMs = System.currentTimeMillis() + tokenResult.expiresInSec * 1000,
                email = profile.email,
                displayName = profile.displayName,
            )
            tokenStorage.clearPendingFlow()

            val existing = accountRepository.getById(accountId.value)
            accountRepository.upsert(
                (existing ?: AccountRecord(
                    accountId = accountId.value,
                    emailAddress = profile.email,
                    displayName = profile.displayName,
                    createdAtEpochMs = System.currentTimeMillis(),
                )).copy(
                    emailAddress = profile.email,
                    displayName = profile.displayName ?: existing?.displayName,
                    connectionState = ConnectionState.CONNECTED,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )

            _authStates.value = _authStates.value + (accountId to GoogleAuthState.Connected(
                accountId = accountId,
                email = profile.email,
                displayName = profile.displayName,
            ))
            MoLogger.i(TAG, "Google account connected: ${profile.email}")
            AuthResult.Success(accountId, profile.email, profile.displayName)
        } catch (e: Exception) {
            tokenStorage.clearPendingFlow()
            MoLogger.e(TAG, "Finishing OAuth connection failed", e)
            AuthResult.Failure(AuthErrorReason.UNKNOWN)
        }
    }

    /**
     * Minimal authenticated Gmail API call: GET /gmail/v1/users/me/profile.
     * Proves OAuth + Gmail authorization work without syncing any mail.
     * Returns null on any failure (caller maps to a safe error).
     */
    private fun fetchGmailProfile(accessToken: String): GmailProfile? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("$gmailApiBase/gmail/v1/users/me/profile")
                .openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            // The token is sent only to Google over HTTPS, never logged.
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            if (connection.responseCode != 200) {
                MoLogger.w(TAG, "Gmail profile returned HTTP ${connection.responseCode}")
                return null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val email = GoogleOAuthHttp.parseFlatJsonObject(body)
                ?.get("emailAddress").orEmpty()
            if (email.isBlank()) return null
            GmailProfile(
                email = email,
                // displayName isn't in the Gmail profile; the People API is
                // out of scope for Phase 3 — use the local part as a label.
                displayName = email.substringBefore("@"),
            )
        } catch (e: Exception) {
            MoLogger.w(TAG, "Gmail profile fetch failed: ${e.javaClass.simpleName}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    override suspend fun disconnect(accountId: AccountId) {
        // Best-effort Google revocation (Phase 3 §23): even if it fails,
        // local state is always cleared so the app stops using the grant.
        try {
            val refreshToken = tokenStorage.getRefreshToken(accountId)
            val accessToken = tokenStorage.getAccessToken(accountId)
            val revoked = when {
                refreshToken != null -> oauthHttp.revokeToken(refreshToken)
                accessToken != null -> oauthHttp.revokeToken(accessToken)
                else -> true
            }
            if (!revoked) {
                MoLogger.w(TAG, "Google revocation did not confirm; local state cleared anyway")
            }
        } catch (e: Exception) {
            MoLogger.w(TAG, "Revocation attempt failed: ${e.javaClass.simpleName}")
        }
        tokenStorage.clearTokens(accountId)
        _authStates.value = _authStates.value - accountId
        // Phase 3 §22: mark disconnected; local mail is preserved (no
        // silent deletion — purge is an explicit user choice in Privacy).
        try {
            accountRepository.updateConnectionState(accountId.value, ConnectionState.DISCONNECTED)
        } catch (e: Exception) {
            MoLogger.w(TAG, "Could not mark account disconnected: ${e.javaClass.simpleName}")
        }
        MoLogger.i(TAG, "Account disconnected: ${accountId.value}")
    }

    override suspend fun isConnected(accountId: AccountId): Boolean =
        tokenStorage.hasTokens(accountId)

    private suspend fun refreshAccessToken(accountId: AccountId): String? {
        val refreshToken = tokenStorage.getRefreshToken(accountId)
        if (refreshToken == null) {
            markAuthError(accountId, AuthErrorReason.REFRESH_FAILED)
            return null
        }
        return when (
            val result = oauthHttp.refreshAccessToken(refreshToken, oauthClientId)
        ) {
            is OAuthTokenResult.Failure -> {
                MoLogger.w(TAG, "Token refresh failed")
                if (result.error == OAuthError.INVALID_GRANT) {
                    // Refresh token revoked/expired — purge so we don't
                    // keep failing; user must reconnect.
                    tokenStorage.clearTokens(accountId)
                    markAuthError(accountId, AuthErrorReason.REVOKED)
                } else {
                    markAuthError(accountId, AuthErrorReason.REFRESH_FAILED)
                }
                null
            }
            is OAuthTokenResult.Success -> {
                tokenStorage.updateAccessToken(
                    accountId = accountId,
                    accessToken = result.accessToken,
                    expiresAtEpochMs = System.currentTimeMillis() + result.expiresInSec * 1000,
                )
                // A rotated refresh token replaces the old one.
                if (result.refreshToken != null) {
                    val email = tokenStorage.getEmail(accountId)
                    val displayName = tokenStorage.getDisplayName(accountId)
                    if (email != null) {
                        tokenStorage.storeTokens(
                            accountId = accountId,
                            accessToken = result.accessToken,
                            refreshToken = result.refreshToken,
                            expiresAtEpochMs = System.currentTimeMillis() + result.expiresInSec * 1000,
                            email = email,
                            displayName = displayName,
                        )
                    }
                }
                result.accessToken
            }
        }
    }

    private suspend fun markAuthError(accountId: AccountId, reason: AuthErrorReason) {
        val email = tokenStorage.getEmail(accountId) ?: return
        _authStates.value = _authStates.value + (accountId to
            GoogleAuthState.AuthError(accountId, email, reason))
        try {
            accountRepository.updateConnectionState(accountId.value, ConnectionState.TOKEN_EXPIRED)
        } catch (e: Exception) {
            MoLogger.w(TAG, "Could not mark token-expired: ${e.javaClass.simpleName}")
        }
    }

    private fun mapOAuthError(error: OAuthError): AuthErrorReason = when (error) {
        OAuthError.INVALID_GRANT -> AuthErrorReason.REFRESH_FAILED
        OAuthError.INVALID_CLIENT -> AuthErrorReason.UNKNOWN
        OAuthError.INVALID_REQUEST -> AuthErrorReason.UNKNOWN
        OAuthError.MALFORMED_RESPONSE -> AuthErrorReason.UNKNOWN
        OAuthError.SERVER_ERROR -> AuthErrorReason.NETWORK_ERROR
        OAuthError.NETWORK -> AuthErrorReason.NETWORK_ERROR
    }

    companion object {
        private const val TAG = "GoogleAuthRepository"
        private const val REFRESH_BUFFER_MS = 5 * 60 * 1000L

        /**
         * Custom-scheme redirect. MUST match the manifest intent-filter
         * (scheme com.greninjaop.mailorganizer, host oauth2redirect).
         */
        const val REDIRECT_URI = "com.greninjaop.mailorganizer://oauth2redirect"
        const val REDIRECT_SCHEME = "com.greninjaop.mailorganizer"

        /**
         * Phase 3 least-privilege scopes (Phase 3 §12): read-only Gmail.
         * Write/integration scopes arrive incrementally in their phases.
         */
        val BASE_SCOPES: List<String> = listOf(
            "https://www.googleapis.com/auth/gmail.readonly",
            "openid",
            "email",
            "profile",
        )

        /** Incremental scopes for later phases (never requested in Phase 3). */
        const val SCOPE_GMAIL_MODIFY = "https://www.googleapis.com/auth/gmail.modify"
        const val SCOPE_CALENDAR_EVENTS = "https://www.googleapis.com/auth/calendar.events"
        const val SCOPE_TASKS = "https://www.googleapis.com/auth/tasks"

        /**
         * Stable account id derived from the Google email address
         * (Phase 3 §45: never create duplicates for the same account).
         * Pure function — unit-tested.
         */
        fun deriveAccountId(email: String): AccountId {
            val normalized = email.trim().lowercase()
            val digest = MessageDigest.getInstance("SHA-256")
            val hex = digest.digest(normalized.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            return AccountId("google_$hex")
        }

        /**
         * Builds the authorization URL (pure JVM logic, unit-tested).
         * Deliberately avoids android.net.Uri so unit tests run without
         * Robolectric.
         */
        fun buildAuthorizationUrl(
            clientId: String,
            redirectUri: String,
            scopes: List<String>,
            codeChallenge: String,
            state: String,
            includeGrantedScopes: Boolean = true,
        ): String {
            fun enc(value: String): String =
                java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
            val params = listOf(
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "response_type" to "code",
                "scope" to scopes.joinToString(" "),
                "code_challenge" to codeChallenge,
                "code_challenge_method" to "S256",
                "state" to state,
                "access_type" to "offline",
                "include_granted_scopes" to includeGrantedScopes.toString(),
                "prompt" to "consent",
            ).joinToString("&") { (k, v) -> "$k=${enc(v)}" }
            return "https://accounts.google.com/o/oauth2/v2/auth?$params"
        }
    }
}

/** Minimal Gmail profile (Phase 3 §27 smoke test result). */
private data class GmailProfile(
    val email: String,
    val displayName: String?,
)
