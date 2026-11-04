package com.wordpilot.assistant.engine.orchestration

import org.junit.Assert.*
import org.junit.Test

class QuotaTrackerTest {

    @Test
    fun `initial state has zero usage and not rate limited`() {
        val tracker = QuotaTracker()
        assertEquals(0L, tracker.getDailyCharsUsed("sapling"))
        assertEquals(0L, tracker.getDailyRequestsUsed("sapling"))
        assertFalse(tracker.isRateLimited("sapling"))
        assertFalse(tracker.isQuotaExhausted("sapling"))
    }

    @Test
    fun `recordRequest increments chars and requests accurately`() {
        val tracker = QuotaTracker()
        tracker.recordRequest("sapling", 150)
        tracker.recordRequest("sapling", 250)
        assertEquals(400L, tracker.getDailyCharsUsed("sapling"))
        assertEquals(2L, tracker.getDailyRequestsUsed("sapling"))
    }

    @Test
    fun `markRateLimited blocks provider until cleared`() {
        val tracker = QuotaTracker()
        tracker.markRateLimited("sapling", retryAfterSeconds = 60)
        assertTrue(tracker.isRateLimited("sapling"))

        tracker.clearRateLimit("sapling")
        assertFalse(tracker.isRateLimited("sapling"))
    }

    @Test
    fun `expired rate limit automatically clears`() {
        val tracker = QuotaTracker()
        // Negative retryAfter = already expired
        tracker.markRateLimited("sapling", retryAfterSeconds = -1)
        assertFalse(tracker.isRateLimited("sapling"))
    }

    @Test
    fun `markQuotaExhausted tracks quota state`() {
        val tracker = QuotaTracker()
        tracker.markQuotaExhausted("sapling", "Daily limit reached")
        assertTrue(tracker.isQuotaExhausted("sapling"))

        tracker.clearQuotaExhausted("sapling")
        assertFalse(tracker.isQuotaExhausted("sapling"))
    }

    @Test
    fun `different providers track quotas independently`() {
        val tracker = QuotaTracker()
        tracker.recordRequest("sapling", 500)
        tracker.recordRequest("languagetool", 100)
        tracker.markRateLimited("sapling", 30)

        assertEquals(500L, tracker.getDailyCharsUsed("sapling"))
        assertEquals(100L, tracker.getDailyCharsUsed("languagetool"))
        assertTrue(tracker.isRateLimited("sapling"))
        assertFalse(tracker.isRateLimited("languagetool"))
    }
}
