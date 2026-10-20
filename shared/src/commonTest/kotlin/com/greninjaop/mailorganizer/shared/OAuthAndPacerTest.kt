package com.greninjaop.mailorganizer.shared

import com.greninjaop.mailorganizer.shared.oauth.OAuthContracts
import com.greninjaop.mailorganizer.shared.sync.TokenBucket
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OAuthAndPacerTest {
    @Test
    fun oauthConstants() {
        assertTrue(OAuthContracts.baseExcludesModify())
        assertTrue(OAuthContracts.REDIRECT_URI.endsWith("/oauth2redirect"))
        assertTrue(OAuthContracts.PKCE_METHOD == "S256")
    }

    @Test
    fun pacerWorks() {
        val b = TokenBucket()
        assertTrue(b.tryTake(0))
        repeat(20) { b.tryTake(0) }
        assertFalse(b.tryTake(0))
        assertTrue(b.tryTake(200))
    }
}
