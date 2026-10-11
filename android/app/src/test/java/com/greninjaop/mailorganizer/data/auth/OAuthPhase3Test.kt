package com.greninjaop.mailorganizer.data.auth

import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.TransportResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3 unit tests: PKCE, authorization URL, account-id derivation,
 * token-response parsing, error mapping, and the full HTTP exchange /
 * refresh / revoke flow against a local fake Google endpoint.
 *
 * Everything here is pure JVM — no android.jar stubs, no Robolectric.
 */
class OAuthPhase3Test {

    // ---------- PkceUtil ----------

    @Test
    fun `verifier has correct length and url-safe alphabet`() {
        val verifier = PkceUtil.generateCodeVerifier()
        // 64 bytes -> 86 base64url chars, no padding.
        assertEquals(86, verifier.length)
        assertTrue(verifier.matches(Regex("[A-Za-z0-9_-]+")))
    }

    @Test
    fun `verifier is unique per call`() {
        assertNotEquals(PkceUtil.generateCodeVerifier(), PkceUtil.generateCodeVerifier())
    }

    @Test
    fun `challenge matches RFC 7636 test vector`() {
        // RFC 7636 Appendix B.
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            PkceUtil.codeChallenge(verifier),
        )
    }

    @Test
    fun `challenge is deterministic for a verifier`() {
        val verifier = PkceUtil.generateCodeVerifier()
        assertEquals(PkceUtil.codeChallenge(verifier), PkceUtil.codeChallenge(verifier))
    }

    @Test
    fun `state is unique and url-safe`() {
        val a = PkceUtil.generateState()
        val b = PkceUtil.generateState()
        assertNotEquals(a, b)
        assertTrue(a.matches(Regex("[A-Za-z0-9_-]+")))
        // 32 bytes -> 43 chars.
        assertEquals(43, a.length)
    }

    // ---------- Authorization URL ----------

    @Test
    fun `authorization url carries PKCE S256 and state`() {
        val url = GoogleAuthRepository.buildAuthorizationUrl(
            clientId = "test-client-id",
            redirectUri = GoogleAuthRepository.REDIRECT_URI,
            scopes = GoogleAuthRepository.BASE_SCOPES,
            codeChallenge = "challenge123",
            state = "state123",
        )
        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("code_challenge=challenge123"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("state=state123"))
        assertTrue(url.contains("access_type=offline"))
        assertTrue(url.contains("prompt=consent"))
        assertTrue(url.contains("include_granted_scopes=true"))
    }

    @Test
    fun `authorization url encodes redirect uri and scopes`() {
        val url = GoogleAuthRepository.buildAuthorizationUrl(
            clientId = "cid",
            redirectUri = "com.example.app://oauth2redirect",
            scopes = listOf("https://www.googleapis.com/auth/gmail.readonly", "email"),
            codeChallenge = "c",
            state = "s",
        )
        // The custom scheme's :// must be percent-encoded in the query.
        assertTrue(url.contains("redirect_uri=com.example.app%3A%2F%2Foauth2redirect"))
        assertTrue(url.contains("gmail.readonly"))
    }

    @Test
    fun `redirect uri matches manifest intent filter`() {
        // Manifest declares scheme=com.greninjaop.mailorganizer,
        // host=oauth2redirect. The code's URI must resolve the same way.
        val uri = GoogleAuthRepository.REDIRECT_URI
        assertTrue(uri.startsWith("com.greninjaop.mailorganizer://"))
        assertTrue(uri.endsWith("oauth2redirect"))
    }

    @Test
    fun `phase 3 base scopes are read-only`() {
        val scopes = GoogleAuthRepository.BASE_SCOPES
        assertTrue(scopes.contains("https://www.googleapis.com/auth/gmail.readonly"))
        // Write / integration scopes must NOT be in the Phase 3 base set.
        assertTrue(scopes.none { it.contains("gmail.modify") })
        assertTrue(scopes.none { it.contains("calendar") })
        assertTrue(scopes.none { it.contains("tasks") })
    }

    // ---------- Account id derivation ----------

    @Test
    fun `deriveAccountId is stable`() {
        val a = GoogleAuthRepository.deriveAccountId("user@example.com")
        val b = GoogleAuthRepository.deriveAccountId("user@example.com")
        assertEquals(a, b)
        assertTrue(a.value.startsWith("google_"))
    }

    @Test
    fun `deriveAccountId is case-insensitive`() {
        assertEquals(
            GoogleAuthRepository.deriveAccountId("User@Example.COM"),
            GoogleAuthRepository.deriveAccountId("user@example.com"),
        )
    }

    @Test
    fun `deriveAccountId differs per email`() {
        assertNotEquals(
            GoogleAuthRepository.deriveAccountId("a@example.com"),
            GoogleAuthRepository.deriveAccountId("b@example.com"),
        )
    }

    // ---------- Flat JSON parser ----------

    @Test
    fun `parseFlatJsonObject reads string and number fields`() {
        val fields = GoogleOAuthHttp.parseFlatJsonObject(
            """{"access_token":"ya.123","expires_in":3599,"scope":"email"}""",
        )
        assertNotNull(fields)
        assertEquals("ya.123", fields!!["access_token"])
        assertEquals("3599", fields["expires_in"])
        assertEquals("email", fields["scope"])
    }

    @Test
    fun `parseFlatJsonObject handles escapes`() {
        val fields = GoogleOAuthHttp.parseFlatJsonObject(
            """{"error_description":"line1\nline2 \"quoted\""}""",
        )
        assertEquals("line1\nline2 \"quoted\"", fields!!["error_description"])
    }

    @Test
    fun `parseFlatJsonObject rejects malformed input`() {
        assertNull(GoogleOAuthHttp.parseFlatJsonObject("not json"))
        assertNull(GoogleOAuthHttp.parseFlatJsonObject("""{"a":}"""))
        assertNull(GoogleOAuthHttp.parseFlatJsonObject("""{"a" "b"}"""))
        assertNull(GoogleOAuthHttp.parseFlatJsonObject(""))
    }

    // ---------- Token response parsing ----------

    @Test
    fun `parseTokenResponse succeeds on valid body`() {
        val http = GoogleOAuthHttp()
        val result = http.parseTokenResponse(
            """{"access_token":"ya.access","refresh_token":"1//refresh","expires_in":3599,"scope":"email","token_type":"Bearer"}""",
        )
        val success = result as OAuthTokenResult.Success
        assertEquals("ya.access", success.accessToken)
        assertEquals("1//refresh", success.refreshToken)
        assertEquals(3599L, success.expiresInSec)
    }

    @Test
    fun `parseTokenResponse keeps null refresh token for refresh flows`() {
        val http = GoogleOAuthHttp()
        val result = http.parseTokenResponse(
            """{"access_token":"ya.access","expires_in":3599}""",
        )
        val success = result as OAuthTokenResult.Success
        assertNull(success.refreshToken)
        assertEquals(3599L, success.expiresInSec)
    }

    @Test
    fun `parseTokenResponse fails without access token`() {
        val http = GoogleOAuthHttp()
        val result = http.parseTokenResponse("""{"expires_in":3599}""")
        assertTrue(result is OAuthTokenResult.Failure)
        assertEquals(
            OAuthError.MALFORMED_RESPONSE,
            (result as OAuthTokenResult.Failure).error,
        )
    }

    @Test
    fun `parseTokenResponse fails on garbage`() {
        val http = GoogleOAuthHttp()
        assertTrue(http.parseTokenResponse("<html>nope</html>") is OAuthTokenResult.Failure)
    }

    // ---------- Error mapping ----------

    @Test
    fun `mapTokenError maps google error codes`() {
        val http = GoogleOAuthHttp()
        assertEquals(
            OAuthError.INVALID_GRANT,
            http.mapTokenError("""{"error":"invalid_grant"}""", 400),
        )
        assertEquals(
            OAuthError.INVALID_CLIENT,
            http.mapTokenError("""{"error":"invalid_client"}""", 401),
        )
        assertEquals(
            OAuthError.SERVER_ERROR,
            http.mapTokenError("""{"error":"internal"}""", 500),
        )
        assertEquals(
            OAuthError.INVALID_GRANT,
            http.mapTokenError("", 400),
        )
    }

    // ---------- Live HTTP flow against a fake token endpoint ----------

    private fun fakeTransport(
        handler: (method: String, url: String, body: String?) -> Pair<Int, String>,
    ): HttpTransport = HttpTransport { method, url, _, body ->
        val (code, response) = handler(method, url, body)
        TransportResponse(code, response)
    }

    private fun httpFor(
        handler: (method: String, url: String, body: String?) -> Pair<Int, String>,
    ): GoogleOAuthHttp = GoogleOAuthHttp(transport = fakeTransport(handler))

    @Test
    fun `exchangeCode posts code and verifier and parses tokens`() = runBlocking {
        val http = httpFor { _, url, body ->
            assertTrue(url.endsWith("/token"))
            assertTrue(body?.contains("grant_type=authorization_code") == true)
            assertTrue(body?.contains("code=authcode123") == true)
            assertTrue(body?.contains("code_verifier=") == true)
            assertTrue(body?.contains("client_id=") == true)
            200 to """{"access_token":"ya.at","refresh_token":"rt","expires_in":3600}"""
        }
        val result = http.exchangeCode("authcode123", "verifier", "cid", "redir")
        val success = result as OAuthTokenResult.Success
        assertEquals("ya.at", success.accessToken)
        assertEquals("rt", success.refreshToken)
    }

    @Test
    fun `exchangeCode maps invalid_grant`() = runBlocking {
        val http = httpFor { _, _, _ ->
            400 to """{"error":"invalid_grant","error_description":"Bad code"}"""
        }
        val result = http.exchangeCode("bad", "v", "cid", "redir")
        assertEquals(
            OAuthError.INVALID_GRANT,
            (result as OAuthTokenResult.Failure).error,
        )
    }

    @Test
    fun `refreshAccessToken posts refresh grant`() = runBlocking {
        val http = httpFor { _, _, body ->
            assertTrue(body?.contains("grant_type=refresh_token") == true)
            assertTrue(body?.contains("refresh_token=old-refresh") == true)
            200 to """{"access_token":"ya.new","expires_in":3600}"""
        }
        val result = http.refreshAccessToken("old-refresh", "cid")
        val success = result as OAuthTokenResult.Success
        assertEquals("ya.new", success.accessToken)
        assertNull(success.refreshToken)
    }

    @Test
    fun `revokeToken returns true on 200`() = runBlocking {
        val http = httpFor { _, url, _ ->
            assertTrue(url.contains("/revoke"))
            200 to "{}"
        }
        assertTrue(http.revokeToken("some-token"))
    }

    @Test
    fun `revokeToken returns false on failure`() = runBlocking {
        val http = httpFor { _, _, _ -> 400 to "{}" }
        assertTrue(!http.revokeToken("some-token"))
    }
}
