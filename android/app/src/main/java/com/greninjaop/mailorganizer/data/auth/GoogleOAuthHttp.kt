package com.greninjaop.mailorganizer.data.auth

import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.UrlConnectionTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URLEncoder

/**
 * Minimal HTTPS client for Google's OAuth2 endpoints (Phase 3 §5, §18).
 *
 * Uses [HttpTransport] (default [UrlConnectionTransport]) — no heavy
 * Google client libraries — and always talks to Google over HTTPS. Raw
 * tokens are never logged; [MoLogger] redacts bearer material as a second
 * layer of defense.
 *
 * [tokenEndpointBase] is injectable so unit tests can point the client at
 * a fake transport. Production code always uses the default
 * `https://oauth2.googleapis.com`.
 */
class GoogleOAuthHttp(
    private val tokenEndpointBase: String = "https://oauth2.googleapis.com",
    private val transport: HttpTransport = UrlConnectionTransport(),
) {

    /**
     * Exchanges an authorization [code] for tokens (RFC 6749 §4.1.3).
     */
    suspend fun exchangeCode(
        code: String,
        codeVerifier: String,
        clientId: String,
        redirectUri: String,
    ): OAuthTokenResult = withContext(Dispatchers.IO) {
        val params = mapOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "code_verifier" to codeVerifier,
        )
        postForm("$tokenEndpointBase/token", params)
    }

    /**
     * Refreshes an access token (RFC 6749 §6).
     */
    suspend fun refreshAccessToken(
        refreshToken: String,
        clientId: String,
    ): OAuthTokenResult = withContext(Dispatchers.IO) {
        val params = mapOf(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to clientId,
        )
        postForm("$tokenEndpointBase/token", params)
    }

    /**
     * Revokes a token (RFC 7009). Best-effort: returns false on any
     * failure; callers must still clear local state.
     */
    suspend fun revokeToken(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$tokenEndpointBase/revoke?token=${urlEncode(token)}"
            val response = transport.execute("POST", url, emptyMap(), null)
            response.code == 200
        } catch (e: Exception) {
            MoLogger.w(TAG, "Token revocation request failed: ${e.javaClass.simpleName}")
            false
        }
    }

    private fun postForm(endpoint: String, params: Map<String, String>): OAuthTokenResult {
        val body = params.entries.joinToString("&") { (k, v) -> "$k=${urlEncode(v)}" }
        try {
            val response = transport.execute(
                method = "POST",
                url = endpoint,
                headers = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Accept" to "application/json",
                ),
                body = body,
            )
            val responseCode = response.code
            val responseBody = response.body.take(MAX_BODY_CHARS)
            if (responseCode == 200) {
                return parseTokenResponse(responseBody)
            }
            return OAuthTokenResult.Failure(mapTokenError(responseBody, responseCode))
        } catch (e: IOException) {
            MoLogger.w(TAG, "OAuth HTTP call failed: ${e.javaClass.simpleName}")
            return OAuthTokenResult.Failure(OAuthError.NETWORK)
        }
    }

    /**
     * Parses a successful token response. Pure function — unit-tested.
     */
    fun parseTokenResponse(body: String): OAuthTokenResult {
        val fields = parseFlatJsonObject(body)
        if (fields == null) {
            MoLogger.w(TAG, "Token response is not valid JSON")
            return OAuthTokenResult.Failure(OAuthError.MALFORMED_RESPONSE)
        }
        val accessToken = fields["access_token"].orEmpty()
        if (accessToken.isBlank()) {
            MoLogger.w(TAG, "Token response missing access_token")
            return OAuthTokenResult.Failure(OAuthError.MALFORMED_RESPONSE)
        }
        return OAuthTokenResult.Success(
            accessToken = accessToken,
            // Refresh responses may omit a new refresh token; callers
            // must keep the previous one in that case.
            refreshToken = fields["refresh_token"]?.ifBlank { null },
            expiresInSec = fields["expires_in"]?.toLongOrNull() ?: DEFAULT_EXPIRES_IN_SEC,
            scope = fields["scope"]?.ifBlank { null },
            tokenType = fields["token_type"]?.ifBlank { "Bearer" } ?: "Bearer",
        )
    }

    /**
     * Maps a token-endpoint error body to a safe [OAuthError]. Pure function.
     */
    fun mapTokenError(body: String, httpCode: Int): OAuthError {
        val googleError = parseFlatJsonObject(body)?.get("error").orEmpty()
        return when (googleError) {
            "invalid_grant" -> OAuthError.INVALID_GRANT
            "invalid_client" -> OAuthError.INVALID_CLIENT
            "unauthorized_client" -> OAuthError.INVALID_CLIENT
            "invalid_request" -> OAuthError.INVALID_REQUEST
            else -> when (httpCode) {
                400, 401 -> OAuthError.INVALID_GRANT
                else -> OAuthError.SERVER_ERROR
            }
        }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    companion object {
        private const val TAG = "GoogleOAuthHttp"
        private const val MAX_BODY_CHARS = 8_192
        private const val DEFAULT_EXPIRES_IN_SEC = 3_600L

        /**
         * Minimal parser for flat JSON objects (`{"k":"v","n":123}`).
         * Deliberately dependency-free (no org.json — that's an android.jar
         * stub in unit tests). Only string keys and string/number/boolean/
         * null values at the top level are supported; anything else yields
         * null. Pure function — unit-tested.
         */
        fun parseFlatJsonObject(body: String): Map<String, String>? {
            val s = body.trim()
            if (!s.startsWith("{") || !s.endsWith("}")) return null
            val result = mutableMapOf<String, String>()
            var i = 1
            val end = s.length - 1
            fun skipWs() { while (i < end && s[i].isWhitespace()) i++ }
            fun parseString(): String? {
                if (i >= end || s[i] != '"') return null
                i++
                val sb = StringBuilder()
                while (i < end) {
                    val c = s[i]
                    if (c == '"') { i++; return sb.toString() }
                    if (c == '\\') {
                        i++
                        if (i >= end) return null
                        when (s[i]) {
                            '"', '\\', '/' -> sb.append(s[i])
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 >= end) return null
                                val hex = s.substring(i + 1, i + 5)
                                val code = hex.toIntOrNull(16) ?: return null
                                sb.append(code.toChar())
                                i += 4
                            }
                            else -> return null
                        }
                        i++
                    } else {
                        sb.append(c)
                        i++
                    }
                }
                return null
            }
            skipWs()
            if (i < end && s[i] == '}') return result
            while (true) {
                skipWs()
                val key = parseString() ?: return null
                skipWs()
                if (i >= end || s[i] != ':') return null
                i++
                skipWs()
                val value: String = if (i < end && s[i] == '"') {
                    parseString() ?: return null
                } else {
                    val start = i
                    while (i < end && s[i] != ',' && s[i] != '}') i++
                    s.substring(start, i).trim().ifEmpty { return null }
                }
                result[key] = value
                skipWs()
                if (i > end) return null
                when (s[i]) {
                    ',' -> { i++; continue }
                    '}' -> return result
                    else -> return null
                }
            }
        }
    }
}

/**
 * Result of a token-endpoint call. Never carries raw tokens into logs —
 * [Success] fields are sensitive by convention (see [AuthRepository]).
 */
sealed interface OAuthTokenResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String?,
        val expiresInSec: Long,
        val scope: String?,
        val tokenType: String,
    ) : OAuthTokenResult

    data class Failure(val error: OAuthError) : OAuthTokenResult
}

/**
 * Safe, user-mappable OAuth error categories (Phase 3 §24, §25).
 * No raw Google error payloads cross this boundary.
 */
enum class OAuthError {
    /** Authorization code expired/used, or refresh token revoked. */
    INVALID_GRANT,

    /** Client misconfiguration (wrong client id / redirect). */
    INVALID_CLIENT,

    /** Malformed request (should not happen; indicates a bug). */
    INVALID_REQUEST,

    /** Response could not be parsed. */
    MALFORMED_RESPONSE,

    /** Google returned 5xx or an unrecognized error. */
    SERVER_ERROR,

    /** No connectivity / timeout. */
    NETWORK,
}
