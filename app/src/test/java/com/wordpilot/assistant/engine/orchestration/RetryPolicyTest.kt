package com.wordpilot.assistant.engine.orchestration

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class RetryPolicyTest {

    @Test
    fun `successful block runs once without retry`() = runBlocking {
        val policy = RetryPolicy(maxAttempts = 3, initialDelayMs = 10, maxDelayMs = 50)
        val counter = AtomicInteger(0)

        val result = policy.execute(
            providerId = "test",
            block = {
                counter.incrementAndGet()
                "success"
            },
            shouldRetry = { it != "success" }
        )

        assertEquals("success", result)
        assertEquals(1, counter.get())
    }

    @Test
    fun `retries up to maxAttempts on retryable failure`() = runBlocking {
        val policy = RetryPolicy(maxAttempts = 3, initialDelayMs = 10, maxDelayMs = 50)
        val counter = AtomicInteger(0)

        val result = policy.execute(
            providerId = "test",
            block = {
                counter.incrementAndGet()
                "transient_error"
            },
            shouldRetry = { it == "transient_error" }
        )

        assertEquals("transient_error", result)
        assertEquals(3, counter.get()) // Attempt 1 + Retry 1 + Retry 2 = 3
    }

    @Test
    fun `stops retrying once operation succeeds`() = runBlocking {
        val policy = RetryPolicy(maxAttempts = 3, initialDelayMs = 10, maxDelayMs = 50)
        val counter = AtomicInteger(0)

        val result = policy.execute(
            providerId = "test",
            block = {
                val attempt = counter.incrementAndGet()
                if (attempt == 2) "success" else "retryable"
            },
            shouldRetry = { it == "retryable" }
        )

        assertEquals("success", result)
        assertEquals(2, counter.get())
    }
}
