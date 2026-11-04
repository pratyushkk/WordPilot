package com.wordpilot.assistant.engine.orchestration

import android.util.Log
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-provider quota and rate-limit tracker.
 * In-memory with timestamp-based reset. Persisted across checks via DataStore (see ProviderOrchestrator).
 *
 * Thread-safe using AtomicLong and ConcurrentHashMap.
 */
class QuotaTracker {

    companion object {
        private const val TAG = "QuotaTracker"
    }

    private data class QuotaState(
        val dailyCharsUsed: AtomicLong = AtomicLong(0),
        val dailyRequestsUsed: AtomicLong = AtomicLong(0),
        val isRateLimited: Boolean = false,
        val rateLimitResetAt: Instant? = null,
        val isQuotaExhausted: Boolean = false,
        val quotaResetAt: Instant? = null,
        val dayStartedAt: Instant = Instant.now()
    )

    private val states = ConcurrentHashMap<String, QuotaState>()

    private fun getOrCreate(providerId: String): QuotaState {
        return states.getOrPut(providerId) { QuotaState() }
    }

    fun recordRequest(providerId: String, charCount: Int) {
        val state = getOrCreate(providerId)
        state.dailyCharsUsed.addAndGet(charCount.toLong())
        state.dailyRequestsUsed.incrementAndGet()
        Log.d(TAG, "[$providerId] charCount=$charCount, daily total=${state.dailyCharsUsed.get()}")
    }

    fun markRateLimited(providerId: String, retryAfterSeconds: Long?) {
        val resetAt = retryAfterSeconds?.let { Instant.now().plusSeconds(it) }
        states[providerId] = getOrCreate(providerId).copy(
            isRateLimited = true,
            rateLimitResetAt = resetAt
        )
        Log.w(TAG, "[$providerId] Rate limited. Reset at: $resetAt")
    }

    fun markQuotaExhausted(providerId: String, resetInfo: String? = null) {
        states[providerId] = getOrCreate(providerId).copy(isQuotaExhausted = true)
        Log.w(TAG, "[$providerId] Quota exhausted. Info: $resetInfo")
    }

    fun clearRateLimit(providerId: String) {
        val existing = states[providerId] ?: return
        states[providerId] = existing.copy(isRateLimited = false, rateLimitResetAt = null)
    }

    fun clearQuotaExhausted(providerId: String) {
        val existing = states[providerId] ?: return
        states[providerId] = existing.copy(isQuotaExhausted = false, quotaResetAt = null)
    }

    fun isRateLimited(providerId: String): Boolean {
        val state = states[providerId] ?: return false
        if (!state.isRateLimited) return false
        val resetAt = state.rateLimitResetAt ?: return true // no reset info → assume still limited
        return if (Instant.now().isAfter(resetAt)) {
            clearRateLimit(providerId)
            false
        } else true
    }

    fun isQuotaExhausted(providerId: String): Boolean {
        return states[providerId]?.isQuotaExhausted ?: false
    }

    fun getDailyCharsUsed(providerId: String): Long = states[providerId]?.dailyCharsUsed?.get() ?: 0
    fun getDailyRequestsUsed(providerId: String): Long = states[providerId]?.dailyRequestsUsed?.get() ?: 0

    fun resetDailyStats(providerId: String) {
        val existing = states[providerId] ?: return
        existing.dailyCharsUsed.set(0)
        existing.dailyRequestsUsed.set(0)
    }
}
