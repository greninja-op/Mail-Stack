package com.greninjaop.mailorganizer.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PKCE (RFC 7636) helpers for the Google OAuth authorization-code flow
 * (Phase 3 §5, §18).
 *
 * A fresh verifier/challenge pair is generated per authorization attempt;
 * the verifier never leaves the device except in the token-exchange POST
 * to Google's token endpoint over HTTPS.
 *
 * All functions are pure and unit-testable.
 */
object PkceUtil {

    private val secureRandom = SecureRandom()

    /**
     * Generates a high-entropy code verifier: 64 random bytes,
     * base64url-encoded without padding (86 chars, within RFC 7636's
     * 43–128 range).
     */
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return base64UrlNoPad(bytes)
    }

    /**
     * Derives the S256 code challenge for [verifier]:
     * BASE64URL-ENCODE(SHA256(ASCII(verifier))).
     */
    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64UrlNoPad(hashed)
    }

    /**
     * Generates an unguessable `state` parameter (32 random bytes,
     * base64url) used for CSRF protection. The redirect handler must
     * reject any response whose state does not match the stored one.
     */
    fun generateState(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return base64UrlNoPad(bytes)
    }

    private fun base64UrlNoPad(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
