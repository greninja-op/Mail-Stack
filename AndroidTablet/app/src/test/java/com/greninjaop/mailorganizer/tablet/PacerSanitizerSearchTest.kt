package com.greninjaop.mailorganizer.tablet

import com.greninjaop.mailorganizer.tablet.data.DemoMailbox
import com.greninjaop.mailorganizer.tablet.privacy.HtmlSanitizer
import com.greninjaop.mailorganizer.tablet.search.StructuredQuery
import com.greninjaop.mailorganizer.tablet.search.StructuredSearch
import com.greninjaop.mailorganizer.tablet.sync.SyncFailureKind
import com.greninjaop.mailorganizer.tablet.sync.TokenBucketPacer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacerSanitizerSearchTest {
    @Test
    fun pacerExhaustThenRefill() {
        val p = TokenBucketPacer()
        assertTrue(p.tryTake(0))
        repeat(20) { p.tryTake(0) }
        assertFalse(p.tryTake(0))
        assertTrue(p.tryTake(200))
    }

    @Test
    fun throttleMessages() {
        val k = SyncFailureKind.classify(429, "rateLimitExceeded")
        assertEquals(SyncFailureKind.PerSecondThrottle, k)
        assertFalse(k.userMessage.contains("Reconnect", ignoreCase = true))
    }

    @Test
    fun sanitizeStrips() {
        val clean = HtmlSanitizer.sanitize("""<p>x</p><script>bad</script><img src="https://x">""")
        assertFalse(clean.contains("script", ignoreCase = true))
        assertFalse(clean.contains("<img", ignoreCase = true))
        assertFalse(HtmlSanitizer.shouldFetchRemoteImages())
    }

    @Test
    fun searchScopedToAccount() {
        val hits = StructuredSearch.search(
            DemoMailbox.threads,
            DemoMailbox.ACCOUNT,
            StructuredQuery(freeText = "security"),
        )
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.accountId == DemoMailbox.ACCOUNT })
    }
}
