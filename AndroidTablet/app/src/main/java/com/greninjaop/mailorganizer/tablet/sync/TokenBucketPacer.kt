package com.greninjaop.mailorganizer.tablet.sync

/** ~10 req/sec pacing for tablet — requirement for new platforms, not phone behaviour. */
class TokenBucketPacer(
    private val capacity: Double = 10.0,
    private val refillPerSec: Double = 10.0,
) {
    private var tokens = capacity
    private var lastMs = 0L

    fun tryTake(nowMs: Long): Boolean {
        if (nowMs > lastMs) {
            val elapsed = (nowMs - lastMs) / 1000.0
            tokens = (tokens + elapsed * refillPerSec).coerceAtMost(capacity)
            lastMs = nowMs
        }
        return if (tokens >= 1.0) {
            tokens -= 1.0
            true
        } else {
            false
        }
    }
}

enum class SyncFailureKind {
    PerSecondThrottle,
    DailyQuota,
    Offline,
    Other,
    ;

    val userMessage: String
        get() = when (this) {
            PerSecondThrottle -> "wait a minute, tap once"
            DailyQuota -> "resets at midnight Pacific"
            Offline -> "You're offline — showing saved mail."
            Other -> "Something went wrong. Try again."
        }

    companion object {
        fun classify(status: Int, body: String): SyncFailureKind {
            val lower = body.lowercase()
            if ("daily" in lower && ("quota" in lower || "limit" in lower)) return DailyQuota
            if (status == 429 || "ratelimit" in lower) return PerSecondThrottle
            if (status == 0) return Offline
            return Other
        }
    }
}
