package com.wordpilot.assistant.engine.orchestration

import org.junit.Assert.*
import org.junit.Test

class CircuitBreakerTest {

    @Test
    fun `initial state is CLOSED and allows calls`() {
        val cb = CircuitBreaker("test-provider", failureThreshold = 3, cooldownSeconds = 60)
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState())
        assertTrue(cb.isCallAllowed())
        assertEquals(0, cb.getConsecutiveFailures())
    }

    @Test
    fun `failures below threshold keep circuit CLOSED`() {
        val cb = CircuitBreaker("test-provider", failureThreshold = 3, cooldownSeconds = 60)
        cb.recordFailure()
        cb.recordFailure()
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState())
        assertTrue(cb.isCallAllowed())
        assertEquals(2, cb.getConsecutiveFailures())
    }

    @Test
    fun `reaching failure threshold opens circuit and blocks calls`() {
        val cb = CircuitBreaker("test-provider", failureThreshold = 3, cooldownSeconds = 60)
        cb.recordFailure()
        cb.recordFailure()
        cb.recordFailure()
        assertEquals(CircuitBreaker.State.OPEN, cb.getState())
        assertFalse(cb.isCallAllowed())
    }

    @Test
    fun `success resets consecutive failure count`() {
        val cb = CircuitBreaker("test-provider", failureThreshold = 3, cooldownSeconds = 60)
        cb.recordFailure()
        cb.recordFailure()
        cb.recordSuccess()
        assertEquals(0, cb.getConsecutiveFailures())
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState())
    }

    @Test
    fun `reset returns circuit to initial clean state`() {
        val cb = CircuitBreaker("test-provider", failureThreshold = 2, cooldownSeconds = 60)
        cb.recordFailure()
        cb.recordFailure()
        assertEquals(CircuitBreaker.State.OPEN, cb.getState())

        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState())
        assertTrue(cb.isCallAllowed())
        assertEquals(0, cb.getConsecutiveFailures())
    }

    @Test
    fun `cooldown expiration transitions from OPEN to HALF_OPEN`() {
        // cooldown = 0 seconds so it expires immediately
        val cb = CircuitBreaker("test-provider", failureThreshold = 1, cooldownSeconds = 0)
        cb.recordFailure()
        assertEquals(CircuitBreaker.State.OPEN, cb.getState())

        // Next call should transition to HALF_OPEN and be allowed as test call
        assertTrue(cb.isCallAllowed())
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState())

        // Success on HALF_OPEN closes circuit
        cb.recordSuccess()
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState())
    }
}
