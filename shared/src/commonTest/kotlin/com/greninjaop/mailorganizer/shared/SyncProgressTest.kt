package com.greninjaop.mailorganizer.shared

import com.greninjaop.mailorganizer.shared.core.MoError
import com.greninjaop.mailorganizer.shared.core.SyncProgress
import com.greninjaop.mailorganizer.shared.core.SyncStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncProgressTest {
    private fun displayFraction(p: SyncProgress): Double = when (p) {
        is SyncProgress.Idle -> 0.0
        is SyncProgress.Running -> {
            val total = p.totalEstimate
            if (total == null || total <= 0) 0.0
            else minOf(0.99, p.messagesProcessed.toDouble() / total.toDouble())
        }
        is SyncProgress.Succeeded -> 1.0
        is SyncProgress.Failed -> 0.0
    }

    @Test
    fun runningCapsAt99() {
        val p = SyncProgress.Running(SyncStage.FETCHING, 100, 100)
        assertEquals(0.99, displayFraction(p), 1e-9)
    }

    @Test
    fun succeededIs100() {
        assertEquals(1.0, displayFraction(SyncProgress.Succeeded(10)), 1e-9)
    }

    @Test
    fun progressNeverDecreases() {
        var last = 0.0
        for (n in listOf(0, 10, 50, 99, 100)) {
            val f = displayFraction(SyncProgress.Running(SyncStage.FETCHING, n, 100))
            assertTrue(f >= last, "decreased $last -> $f")
            last = f
        }
    }

    @Test
    fun rateLimitedIsNotAuthReconnect() {
        val err = MoError.RateLimited("wait a minute, tap once", daily = false)
        assertTrue(!err.message.contains("Reconnect", ignoreCase = true))
        val daily = MoError.RateLimited("resets at midnight Pacific", daily = true)
        assertTrue(daily.daily)
    }
}
