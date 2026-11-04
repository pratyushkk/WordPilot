package com.wordpilot.assistant.engine.orchestration

import com.wordpilot.assistant.engine.providers.CircuitState
import com.wordpilot.assistant.engine.providers.ProviderStatus
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks per-provider health combining circuit state, quota, and rate-limit info.
 */
class ProviderHealthMonitor(
    private val circuitBreakers: Map<String, CircuitBreaker>,
    private val quotaTracker: QuotaTracker
) {
    // providerId → (successCount, failureCount)
    private val requestCounts = ConcurrentHashMap<String, Pair<Long, Long>>()
    private val lastSuccess = ConcurrentHashMap<String, Instant>()
    private val lastFailure = ConcurrentHashMap<String, Instant>()

    fun recordSuccess(providerId: String) {
        val (s, f) = requestCounts.getOrDefault(providerId, Pair(0L, 0L))
        requestCounts[providerId] = Pair(s + 1, f)
        lastSuccess[providerId] = Instant.now()
    }

    fun recordFailure(providerId: String) {
        val (s, f) = requestCounts.getOrDefault(providerId, Pair(0L, 0L))
        requestCounts[providerId] = Pair(s, f + 1)
        lastFailure[providerId] = Instant.now()
    }

    fun getStatus(providerId: String): ProviderStatus {
        val cb = circuitBreakers[providerId]
        val cbState = when (cb?.getState()) {
            CircuitBreaker.State.OPEN      -> CircuitState.OPEN
            CircuitBreaker.State.HALF_OPEN -> CircuitState.HALF_OPEN
            else                           -> CircuitState.CLOSED
        }
        val (success, failed) = requestCounts.getOrDefault(providerId, Pair(0L, 0L))
        return ProviderStatus(
            providerId          = providerId,
            circuitState        = cbState,
            consecutiveFailures = cb?.getConsecutiveFailures() ?: 0,
            lastSuccess         = lastSuccess[providerId],
            lastFailure         = lastFailure[providerId],
            isRateLimited       = quotaTracker.isRateLimited(providerId),
            isQuotaExhausted    = quotaTracker.isQuotaExhausted(providerId),
            totalRequests       = success + failed,
            successfulRequests  = success,
            failedRequests      = failed
        )
    }

    fun getAllStatuses(providerIds: List<String>): Map<String, ProviderStatus> {
        return providerIds.associateWith { getStatus(it) }
    }
}
