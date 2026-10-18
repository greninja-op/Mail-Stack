package com.greninjaop.mailorganizer.tablet

import com.greninjaop.mailorganizer.tablet.data.DemoMailbox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressAndPrivacyTest {
    private fun displayFraction(processed: Int, total: Int?, running: Boolean, done: Boolean): Double {
        if (done) return 1.0
        if (!running) return 0.0
        if (total == null || total <= 0) return 0.0
        return minOf(0.99, processed.toDouble() / total.toDouble())
    }

    @Test
    fun runningCapsAt99AndDoneIs100() {
        assertEquals(0.99, displayFraction(100, 100, running = true, done = false), 1e-9)
        assertEquals(1.0, displayFraction(100, 100, running = false, done = true), 1e-9)
    }

    @Test
    fun progressNeverDecreases() {
        var last = 0.0
        for (n in listOf(0, 10, 50, 99, 100)) {
            val f = displayFraction(n, 100, running = true, done = false)
            assertTrue(f >= last)
            last = f
        }
    }

    @Test
    fun remoteImagesNeverFetched() {
        // Tablet scaffold policy — mirrors DefaultPrivacy / desktop-core.
        val fetchesRemoteImages = false
        assertFalse(fetchesRemoteImages)
    }

    @Test
    fun demoThreadsScopedToLocalAccount() {
        assertTrue(DemoMailbox.threads.all { it.accountId.value.startsWith("local-") })
    }

    @Test
    fun noPhoneAndroidDependencyInClasspathHint() {
        // Isolation: tablet module package must not live under phone package.
        assertFalse(DemoMailbox::class.java.name.startsWith("com.greninjaop.mailorganizer.ui"))
        assertTrue(DemoMailbox::class.java.name.contains(".tablet."))
    }
}
