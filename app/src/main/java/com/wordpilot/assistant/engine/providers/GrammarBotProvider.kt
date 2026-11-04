package com.wordpilot.assistant.engine.providers

import android.util.Log
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GrammarBot Neural via RapidAPI.
 * DISABLED by default — enable only after credentials and contract tests are verified.
 *
 * Computes structured token-level edits from sentence differences.
 */
class GrammarBotProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val endpointUrl: String = DEFAULT_ENDPOINT
) : GrammarProvider {
    override val id = "grammarbot"

    companion object {
        private const val TAG = "GrammarBotProvider"
        const val DEFAULT_ENDPOINT = "https://grammarbot-neural.p.rapidapi.com/v1/check"
        private const val RAPIDAPI_HOST = "grammarbot-neural.p.rapidapi.com"
        private const val MAX_CHARS = 500
    }

    private var config: ProviderConfig? = null

    override fun configure(config: ProviderConfig) {
        this.config = config
    }

    override fun isConfigured(): Boolean = config?.isOperational == true
    override fun isEnabled(): Boolean = config?.enabled == true

    override suspend fun check(text: String, language: String): ProviderResult =
        withContext(Dispatchers.IO) {
            val cfg = config
            if (cfg == null || !cfg.enabled) {
                return@withContext ProviderResult.Failure.Disabled(
                    id, "GrammarBot is disabled by default. Enable it in Settings after verifying RapidAPI credentials."
                )
            }
            val apiKey = cfg.apiKey
            if (apiKey.isNullOrBlank()) {
                return@withContext ProviderResult.Failure.NotConfigured(id, "GrammarBot RapidAPI key is not configured")
            }

            val input = if (text.length > MAX_CHARS) {
                Log.w(TAG, "Input truncated to $MAX_CHARS chars due to conservative GrammarBot limits")
                text.take(MAX_CHARS)
            } else text

            try {
                val formBody = FormBody.Builder()
                    .add("text", input)
                    .add("lang", language)
                    .build()

                val request = Request.Builder()
                    .url(endpointUrl)
                    .addHeader("X-RapidAPI-Key", apiKey)
                    .addHeader("X-RapidAPI-Host", RAPIDAPI_HOST)
                    .post(formBody)
                    .build()

                val response = client.newCall(request).execute()
                val statusCode = response.code
                val responseBody = response.body?.string().orEmpty()

                when {
                    statusCode == 429 -> {
                        val retryAfter = response.header("Retry-After")?.toLongOrNull()
                        return@withContext ProviderResult.Failure.RateLimited(id, "GrammarBot rate limit reached", retryAfter)
                    }
                    statusCode == 401 || statusCode == 403 -> {
                        return@withContext ProviderResult.Failure.AuthError(id, "GrammarBot authentication failure (HTTP $statusCode)", statusCode)
                    }
                    !response.isSuccessful -> {
                        return@withContext ProviderResult.Failure.HttpError(id, "GrammarBot returned HTTP $statusCode", statusCode)
                    }
                }

                parseResponse(responseBody, input)
            } catch (e: java.net.SocketTimeoutException) {
                ProviderResult.Failure.Timeout(id, "GrammarBot request timed out")
            } catch (e: Exception) {
                Log.e(TAG, "GrammarBot error", e)
                ProviderResult.Failure.NetworkError(id, "GrammarBot network error: ${e.javaClass.simpleName}", e)
            }
        }

    private fun parseResponse(responseBody: String, originalText: String): ProviderResult {
        return try {
            val root = JSONObject(responseBody)
            val status = root.optString("status", "")
            if (status.equals("error", ignoreCase = true)) {
                val msg = root.optString("message", "Unknown GrammarBot error")
                return ProviderResult.Failure.InvalidResponse(id, msg)
            }

            val correctedText = root.optString("correction", "")
            if (correctedText.isBlank() || correctedText == originalText) {
                return ProviderResult.Success(emptyList(), id)
            }

            val corrections = computeDiffCorrections(originalText, correctedText)
            ProviderResult.Success(corrections = corrections, providerId = id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse GrammarBot JSON", e)
            ProviderResult.Failure.InvalidResponse(id, "Failed to parse GrammarBot response")
        }
    }

    private data class WordOffset(val word: String, val start: Int, val end: Int)

    private fun computeDiffCorrections(original: String, corrected: String): List<Correction> {
        val originalWords = tokenizeWithOffsets(original)
        val correctedWords = corrected.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (originalWords.size != correctedWords.size) {
            return listOf(
                Correction(
                    startOffset = 0,
                    endOffset = original.length,
                    originalText = original,
                    suggestedReplacement = corrected,
                    category = CorrectionCategory.GRAMMAR,
                    explanation = "Grammar correction",
                    severity = CorrectionSeverity.WARNING,
                    confidence = -1f,
                    ruleId = "GRAMMARBOT_SENTENCE",
                    isSafeToApply = false
                )
            )
        }

        val corrections = mutableListOf<Correction>()
        for (i in originalWords.indices) {
            val (origWord, start, end) = originalWords[i]
            val corrWord = correctedWords.getOrElse(i) { origWord }
            if (origWord != corrWord) {
                corrections.add(
                    Correction(
                        startOffset = start,
                        endOffset = end,
                        originalText = origWord,
                        suggestedReplacement = corrWord,
                        category = CorrectionCategory.GRAMMAR,
                        explanation = "Grammar correction",
                        severity = CorrectionSeverity.WARNING,
                        confidence = -1f,
                        ruleId = "GRAMMARBOT_WORD",
                        isSafeToApply = true
                    )
                )
            }
        }
        return corrections
    }

    private fun tokenizeWithOffsets(text: String): List<WordOffset> {
        val list = mutableListOf<WordOffset>()
        val regex = Regex("\\S+")
        for (m in regex.findAll(text)) {
            list.add(WordOffset(m.value, m.range.first, m.range.last + 1))
        }
        return list
    }
}
