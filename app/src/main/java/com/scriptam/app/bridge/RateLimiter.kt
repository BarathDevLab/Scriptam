package com.scriptam.app.bridge

import java.util.concurrent.ConcurrentHashMap

/**
 * Token-bucket rate limiter to protect against runaway API loops and 429 penalties.
 * Supports global limit as well as per-host rate limiting.
 */
class RateLimiter(
    private val maxTokens: Int = 10,
    private val refillRatePerSecond: Double = 5.0
) {
    private val hostBuckets = ConcurrentHashMap<String, TokenBucket>()
    private val globalBucket = TokenBucket(maxTokens, refillRatePerSecond)

    /**
     * Attempts to acquire a token for the given [host].
     * Returns true if allowed, false if rate limit exceeded.
     */
    fun tryAcquire(host: String = "global"): Boolean {
        if (!globalBucket.tryAcquire()) return false
        val hostBucket = hostBuckets.computeIfAbsent(host) {
            TokenBucket(maxTokens, refillRatePerSecond)
        }
        return hostBucket.tryAcquire()
    }

    private class TokenBucket(
        private val capacity: Int,
        private val refillRate: Double
    ) {
        private var tokens: Double = capacity.toDouble()
        private var lastRefillTimestamp: Long = System.currentTimeMillis()

        @Synchronized
        fun tryAcquire(): Boolean {
            refill()
            return if (tokens >= 1.0) {
                tokens -= 1.0
                true
            } else {
                false
            }
        }

        private fun refill() {
            val now = System.currentTimeMillis()
            val elapsedSeconds = (now - lastRefillTimestamp) / 1000.0
            lastRefillTimestamp = now
            tokens = (tokens + elapsedSeconds * refillRate).coerceAtMost(capacity.toDouble())
        }
    }
}
