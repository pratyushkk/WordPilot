package com.wordpilot.assistant.engine.providers

import com.wordpilot.assistant.domain.models.Correction

sealed class ProviderResult {
    data class Success(
        val corrections: List<Correction>,
        val providerId: String,
        val isComplete: Boolean = true,
        val processingTimeMs: Long = 0
    ) : ProviderResult()

    sealed class Failure : ProviderResult() {
        abstract val providerId: String
        abstract val message: String

        data class NetworkError(override val providerId: String, override val message: String, val cause: Throwable? = null) : Failure()
        data class HttpError(override val providerId: String, override val message: String, val statusCode: Int) : Failure()
        data class RateLimited(override val providerId: String, override val message: String, val retryAfterSeconds: Long?) : Failure()
        data class QuotaExhausted(override val providerId: String, override val message: String, val resetInfo: String? = null) : Failure()
        data class AuthError(override val providerId: String, override val message: String, val statusCode: Int) : Failure()
        data class NotConfigured(override val providerId: String, override val message: String) : Failure()
        data class Disabled(override val providerId: String, override val message: String) : Failure()
        data class Timeout(override val providerId: String, override val message: String) : Failure()
        data class InvalidResponse(override val providerId: String, override val message: String) : Failure()
        data class CircuitOpen(override val providerId: String, override val message: String) : Failure()
    }
}
