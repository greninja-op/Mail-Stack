package com.greninjaop.mailorganizer.shared.sync

/** New-platform pacing (~10/sec). Not a claim about Android phone behaviour. */
class TokenBucket(
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
