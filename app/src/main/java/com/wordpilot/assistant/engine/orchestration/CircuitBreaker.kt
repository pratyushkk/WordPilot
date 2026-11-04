package com.wordpilot.assistant.engine.orchestration

import android.util.Log
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Three-state circuit breaker per provider.
 * CLOSED    → requests pass through normally
 * OPEN      → requests are blocked for cooldown period
 * HALF_OPEN → one test request is allowed; success → CLOSED, failure → OPEN
 */
class CircuitBreaker(
    private val providerId: String,
    private val failureThreshold: Int = 3,
    private val cooldownSeconds: Long = 60L,
    private val halfOpenTestIntervalSeconds: Long = 30L
) {
    private val TAG = "CircuitBreaker[$providerId]"

    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val consecutiveFailures = AtomicInteger(0)
    private val state = AtomicReference(State.CLOSED)
    private val openedAt = AtomicReference<Instant?>(null)
    private val halfOpenTestAt = AtomicReference<Instant?>(null)

    fun isCallAllowed(): Boolean {
        return when (state.get()) {
            State.CLOSED -> true
            State.OPEN -> {
                val opened = openedAt.get() ?: return false
                val cooldownExpired = cooldownSeconds <= 0L || !Instant.now().isBefore(opened.plusSeconds(cooldownSeconds))
                if (cooldownExpired) {
                    // Transition to HALF_OPEN for one test request
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        halfOpenTestAt.set(Instant.now())
                        Log.i(TAG, "Circuit HALF_OPEN — allowing test request")
                    }
                    state.get() == State.HALF_OPEN
                } else false
            }
            State.HALF_OPEN -> {
                // Only allow one test call; block subsequent calls while testing
                val testAt = halfOpenTestAt.get()
                testAt != null && Instant.now().isBefore(testAt.plusSeconds(halfOpenTestIntervalSeconds))
            }
        }
    }

    fun recordSuccess() {
        consecutiveFailures.set(0)
        if (state.compareAndSet(State.HALF_OPEN, State.CLOSED) || state.compareAndSet(State.OPEN, State.CLOSED)) {
            Log.i(TAG, "Circuit CLOSED after successful response")
        }
        openedAt.set(null)
        halfOpenTestAt.set(null)
    }

    fun recordFailure() {
        val failures = consecutiveFailures.incrementAndGet()
        Log.w(TAG, "Failure recorded ($failures/$failureThreshold)")
        if (failures >= failureThreshold || state.get() == State.HALF_OPEN) {
            if (state.compareAndSet(State.CLOSED, State.OPEN) ||
                state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                openedAt.set(Instant.now())
                Log.w(TAG, "Circuit OPEN — cooling down for ${cooldownSeconds}s")
            }
        }
    }

    fun reset() {
        consecutiveFailures.set(0)
        state.set(State.CLOSED)
        openedAt.set(null)
        halfOpenTestAt.set(null)
    }

    fun getState(): State = state.get()
    fun getConsecutiveFailures(): Int = consecutiveFailures.get()
}
