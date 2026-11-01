package com.wordpilot.assistant.engine.orchestration

import android.util.Log
import kotlinx.coroutines.delay

/**
 * Bounded exponential backoff with jitter.
 * Never creates infinite retry loops.
 */
class RetryPolicy(
    private val maxAttempts: Int = 2,        // 1 original + 1 retry = 2 total
    private val initialDelayMs: Long = 500L,
    private val maxDelayMs: Long = 5000L,
    private val multiplier: Double = 2.0,
    private val jitterFactor: Double = 0.2   // ±20% random jitter
) {
    companion object {
        private const val TAG = "RetryPolicy"
    }

    /**
     * Executes [block] with retry logic.
     * Returns the last result (success or failure) after exhausting retries.
     */
    suspend fun <T> execute(
        providerId: String,
        block: suspend () -> T,
        shouldRetry: (T) -> Boolean
    ): T {
        var attempt = 0
        var delayMs = initialDelayMs

        while (true) {
            val result = block()
            if (!shouldRetry(result) || attempt >= maxAttempts - 1) {
                return result
            }

            attempt++
            val jitter = (delayMs * jitterFactor * (Math.random() * 2 - 1)).toLong()
            val actualDelay = (delayMs + jitter).coerceAtLeast(100L)
            Log.d(TAG, "[$providerId] Retry $attempt/${maxAttempts - 1} after ${actualDelay}ms")
            delay(actualDelay)
            delayMs = (delayMs * multiplier).toLong().coerceAtMost(maxDelayMs)
        }
    }
}
