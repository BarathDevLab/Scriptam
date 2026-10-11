package com.scriptam.app.bridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    @Test
    fun testRateLimiterAllowsInitialTokens() {
        val limiter = RateLimiter(maxTokens = 3, refillRatePerSecond = 1.0)
        assertTrue(limiter.tryAcquire("api.example.com"))
        assertTrue(limiter.tryAcquire("api.example.com"))
        assertTrue(limiter.tryAcquire("api.example.com"))
        // 4th request exceeds capacity
        assertFalse(limiter.tryAcquire("api.example.com"))
    }

    @Test
    fun testRateLimiterPerHostIsolation() {
        val limiter = RateLimiter(maxTokens = 2, refillRatePerSecond = 0.1)
        assertTrue(limiter.tryAcquire("hostA.com"))
        assertTrue(limiter.tryAcquire("hostA.com"))
        // Host A exhausted
        assertFalse(limiter.tryAcquire("hostA.com"))

        // Host B is still independent and allowed (within global quota)
        val globalLimiter = RateLimiter(maxTokens = 10, refillRatePerSecond = 0.1)
        globalLimiter.tryAcquire("hostA.com")
        globalLimiter.tryAcquire("hostA.com")
        // Different host
        assertTrue(globalLimiter.tryAcquire("hostB.com"))
    }
}
