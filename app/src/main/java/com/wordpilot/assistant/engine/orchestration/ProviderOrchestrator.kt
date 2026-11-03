package com.wordpilot.assistant.engine.orchestration

import android.util.Log
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.engine.normalization.DuplicateDetector
import com.wordpilot.assistant.engine.providers.GrammarProvider
import com.wordpilot.assistant.engine.providers.ProviderConfig
import com.wordpilot.assistant.engine.providers.ProviderResult
import com.wordpilot.assistant.engine.providers.ProviderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class OrchestratedResult(
    val success: Boolean,
    val complete: Boolean,
    val requestId: String,
    val providerUsed: String?,
    val fallbackUsed: Boolean,
    val corrections: List<Correction>,
    val processingTimeMs: Long,
    val errorMessage: String? = null
)

/**
 * Orchestrates multiple grammar providers with intelligent sequential failover,
 * per-provider circuit breakers, quota tracking, bounded exponential backoff retries,
 * and unified correction deduplication.
 */
class ProviderOrchestrator(
    private val providers: Map<String, GrammarProvider>,
    private var providerPriority: List<String> = listOf("sapling", "languagetool")
) {
    companion object {
        private const val TAG = "ProviderOrchestrator"
    }

    private val circuitBreakers = providers.keys.associateWith { CircuitBreaker(it) }
    val quotaTracker = QuotaTracker()
    val healthMonitor = ProviderHealthMonitor(circuitBreakers, quotaTracker)
    private val retryPolicy = RetryPolicy(maxAttempts = 2)

    fun updatePriority(newPriority: List<String>) {
        providerPriority = newPriority
        Log.d(TAG, "Updated provider priority order: $providerPriority")
    }

    fun getProvider(providerId: String): GrammarProvider? = providers[providerId]

    fun configureProvider(providerId: String, config: ProviderConfig) {
        providers[providerId]?.configure(config)
    }

    fun getProviderStatus(providerId: String): ProviderStatus {
        return healthMonitor.getStatus(providerId)
    }

    fun getAllProviderStatuses(): Map<String, ProviderStatus> {
        return healthMonitor.getAllStatuses(providers.keys.toList())
    }

    suspend fun check(
        text: String,
        language: String = "en",
        requestId: String = UUID.randomUUID().toString()
    ): OrchestratedResult = withContext(Dispatchers.Default) {
        if (text.isBlank()) {
            return@withContext OrchestratedResult(
                success = true,
                complete = true,
                requestId = requestId,
                providerUsed = null,
                fallbackUsed = false,
                corrections = emptyList(),
                processingTimeMs = 0
            )
        }

        val startTime = System.currentTimeMillis()
        var primaryProviderId: String? = null
        var lastFailureReason: String? = null

        // Sequential failover in order of priority
        for (providerId in providerPriority) {
            val provider = providers[providerId] ?: continue

            // Filter out disabled or unconfigured providers
            if (!provider.isEnabled() || !provider.isConfigured()) {
                Log.d(TAG, "[$providerId] Skipped: enabled=${provider.isEnabled()}, configured=${provider.isConfigured()}")
                continue
            }

            // Filter out rate-limited or quota-exhausted providers
            if (quotaTracker.isRateLimited(providerId)) {
                Log.w(TAG, "[$providerId] Skipped: currently rate limited")
                continue
            }
            if (quotaTracker.isQuotaExhausted(providerId)) {
                Log.w(TAG, "[$providerId] Skipped: quota exhausted")
                continue
            }

            // Filter out open circuit breakers
            val circuitBreaker = circuitBreakers[providerId]
            if (circuitBreaker != null && !circuitBreaker.isCallAllowed()) {
                Log.w(TAG, "[$providerId] Skipped: circuit breaker OPEN")
                continue
            }

            if (primaryProviderId == null) {
                primaryProviderId = providerId
            }

            Log.i(TAG, "Attempting grammar check with [$providerId] (chars=${text.length})")

            val result = executeWithRetry(provider, text, language)

            when (result) {
                is ProviderResult.Success -> {
                    circuitBreaker?.recordSuccess()
                    healthMonitor.recordSuccess(providerId)
                    quotaTracker.recordRequest(providerId, text.length)

                    val processedCorrections = DuplicateDetector.process(result.corrections)
                    val duration = System.currentTimeMillis() - startTime
                    val isFallback = (providerId != primaryProviderId)

                    Log.i(TAG, "Success with [$providerId]: ${processedCorrections.size} corrections in ${duration}ms (fallback=$isFallback)")

                    return@withContext OrchestratedResult(
                        success = true,
                        complete = result.isComplete,
                        requestId = requestId,
                        providerUsed = providerId,
                        fallbackUsed = isFallback,
                        corrections = processedCorrections,
                        processingTimeMs = duration
                    )
                }

                is ProviderResult.Failure -> {
                    circuitBreaker?.recordFailure()
                    healthMonitor.recordFailure(providerId)
                    lastFailureReason = "${result.providerId}: ${result.message}"

                    handleFailureSideEffects(result)
                    Log.w(TAG, "Provider [$providerId] failed: ${result.message}. Proceeding to next eligible provider.")
                }
            }
        }

        val totalDuration = System.currentTimeMillis() - startTime
        Log.e(TAG, "All grammar providers failed or exhausted after ${totalDuration}ms. Last error: $lastFailureReason")

        OrchestratedResult(
            success = false,
            complete = false,
            requestId = requestId,
            providerUsed = null,
            fallbackUsed = false,
            corrections = emptyList(),
            processingTimeMs = totalDuration,
            errorMessage = lastFailureReason ?: "All grammar providers unavailable"
        )
    }

    private suspend fun executeWithRetry(
        provider: GrammarProvider,
        text: String,
        language: String
    ): ProviderResult {
        return retryPolicy.execute(
            providerId = provider.id,
            block = { provider.check(text, language) },
            shouldRetry = { result ->
                when (result) {
                    is ProviderResult.Failure.HttpError -> result.statusCode in 500..504
                    is ProviderResult.Failure.Timeout -> true
                    is ProviderResult.Failure.NetworkError -> true
                    else -> false
                }
            }
        )
    }

    private fun handleFailureSideEffects(failure: ProviderResult.Failure) {
        when (failure) {
            is ProviderResult.Failure.RateLimited -> {
                quotaTracker.markRateLimited(failure.providerId, failure.retryAfterSeconds)
            }
            is ProviderResult.Failure.QuotaExhausted -> {
                quotaTracker.markQuotaExhausted(failure.providerId, failure.resetInfo)
            }
            is ProviderResult.Failure.AuthError -> {
                circuitBreakers[failure.providerId]?.recordFailure()
            }
            else -> {
                // Standard failure recorded in health monitor
            }
        }
    }
}
