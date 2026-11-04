package com.wordpilot.assistant.engine.providers

import java.time.Instant

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

data class ProviderStatus(
    val providerId: String,
    val circuitState: CircuitState = CircuitState.CLOSED,
    val consecutiveFailures: Int = 0,
    val lastSuccess: Instant? = null,
    val lastFailure: Instant? = null,
    val isRateLimited: Boolean = false,
    val rateLimitResetAt: Instant? = null,
    val isQuotaExhausted: Boolean = false,
    val quotaResetAt: Instant? = null,
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0
) {
    val isAvailable: Boolean
        get() = circuitState != CircuitState.OPEN && !isRateLimited && !isQuotaExhausted
}
