package com.wordpilot.assistant.engine.providers

import android.util.Log
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.normalization.OffsetValidator
import com.wordpilot.assistant.engine.normalization.SaplingChunker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class SaplingProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val endpointUrl: String = DEFAULT_ENDPOINT
) : GrammarProvider {
    override val id = "sapling"

    companion object {
        private const val TAG = "SaplingProvider"
        const val DEFAULT_ENDPOINT = "https://api.sapling.ai/api/v1/edits"
        private const val MAX_CHUNK_CHARS = 4000
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
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
                return@withContext ProviderResult.Failure.Disabled(id, "Sapling provider is disabled")
            }
            val apiKey = cfg.apiKey
            if (apiKey.isNullOrBlank()) {
                return@withContext ProviderResult.Failure.NotConfigured(id, "Sapling API key not configured")
            }

            val startTime = System.currentTimeMillis()
            val chunks = SaplingChunker.chunk(text, MAX_CHUNK_CHARS)
            val allCorrections = mutableListOf<Correction>()
            var isComplete = true

            for (chunk in chunks) {
                when (val result = checkChunk(chunk.text, chunk.documentOffset, apiKey, language)) {
                    is ProviderResult.Success -> {
                        allCorrections.addAll(result.corrections)
                        if (!result.isComplete) isComplete = false
                    }
                    is ProviderResult.Failure -> {
                        return@withContext result
                    }
                }
            }

            ProviderResult.Success(
                corrections = allCorrections,
                providerId = id,
                isComplete = isComplete,
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }

    private suspend fun checkChunk(
        chunkText: String,
        documentOffset: Int,
        apiKey: String,
        language: String
    ): ProviderResult = withContext(Dispatchers.IO) {
        try {
            val requestJson = JSONObject().apply {
                put("key", apiKey)
                put("text", chunkText)
                put("session_id", UUID.randomUUID().toString())
                put("lang", language.take(2))
                put("variety", "us-variety")
                put("include_error_categories", true)
                put("auto_apply", false)
            }

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val statusCode = response.code
            val responseBody = response.body?.string().orEmpty()

            if (statusCode == 429) {
                val retryAfter = response.header("Retry-After")?.toLongOrNull()
                Log.w(TAG, "Sapling rate limited (HTTP 429). Retry-After: $retryAfter")
                return@withContext ProviderResult.Failure.RateLimited(id, "Rate limited by Sapling", retryAfter)
            }

            if (statusCode == 402) {
                Log.w(TAG, "Sapling quota or billing restriction (HTTP 402)")
                return@withContext ProviderResult.Failure.QuotaExhausted(id, "Sapling daily trial or quota exhausted (HTTP 402)")
            }

            if (statusCode == 401 || statusCode == 403) {
                Log.e(TAG, "Sapling auth error (HTTP $statusCode)")
                return@withContext ProviderResult.Failure.AuthError(id, "Sapling authentication failure (HTTP $statusCode)", statusCode)
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "Sapling error response (HTTP $statusCode): $responseBody")
                return@withContext ProviderResult.Failure.HttpError(id, "Sapling returned HTTP $statusCode", statusCode)
            }

            parseResponse(responseBody, chunkText, documentOffset)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Sapling request timeout", e)
            ProviderResult.Failure.Timeout(id, "Sapling request timed out")
        } catch (e: Exception) {
            Log.e(TAG, "Network error calling Sapling", e)
            ProviderResult.Failure.NetworkError(id, "Sapling network failure: ${e.javaClass.simpleName}", e)
        }
    }

    private fun parseResponse(
        responseBody: String,
        chunkText: String,
        documentOffset: Int
    ): ProviderResult {
        return try {
            val root = JSONObject(responseBody)
            val isDegraded = root.optBoolean("degraded", false)
            val edits = root.optJSONArray("edits") ?: JSONArray()
            val corrections = mutableListOf<Correction>()

            for (i in 0 until edits.length()) {
                val edit = edits.getJSONObject(i)
                val _sentence = edit.optString("sentence", "")
                val sentenceStart = edit.optInt("sentence_start", 0)
                val start = edit.optInt("start", 0)
                val end = edit.optInt("end", 0)
                val replacement = edit.optString("replacement", "")
                val errorType = edit.optString("error_type", "")
                val generalErrorType = edit.optString("general_error_type", "")

                // Sapling official specification: start/end are sentence-relative
                val absoluteStartInChunk = sentenceStart + start
                val absoluteEndInChunk = sentenceStart + end

                val docStart = documentOffset + absoluteStartInChunk
                val docEnd = documentOffset + absoluteEndInChunk

                val validated = OffsetValidator.validate(chunkText, absoluteStartInChunk, absoluteEndInChunk)
                if (validated == null) {
                    Log.w(TAG, "Skipping invalid edit offset: [$absoluteStartInChunk, $absoluteEndInChunk] in chunk of length ${chunkText.length}")
                    continue
                }

                val originalText = try {
                    chunkText.substring(validated.first, validated.second)
                } catch (e: Exception) {
                    continue
                }

                if (replacement.isBlank() && originalText.isBlank()) continue

                val category = mapErrorCategory(generalErrorType, errorType)
                val severity = mapErrorSeverity(generalErrorType)

                corrections.add(
                    Correction(
                        startOffset = docStart,
                        endOffset = docEnd,
                        originalText = originalText,
                        suggestedReplacement = replacement.ifBlank { null },
                        category = category,
                        explanation = buildExplanation(errorType, generalErrorType),
                        severity = severity,
                        confidence = -1f,
                        ruleId = "SAPLING_${generalErrorType.uppercase().replace(' ', '_').ifBlank { "GRAMMAR" }}",
                        isSafeToApply = true
                    )
                )
            }

            ProviderResult.Success(
                corrections = corrections,
                providerId = id,
                isComplete = !isDegraded
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Sapling JSON", e)
            ProviderResult.Failure.InvalidResponse(id, "Failed to parse Sapling response: ${e.message}")
        }
    }

    private fun mapErrorCategory(general: String, specific: String): CorrectionCategory = when {
        general.contains("spell", ignoreCase = true) || specific.contains("spell", ignoreCase = true) -> CorrectionCategory.SPELLING
        general.contains("punct", ignoreCase = true) || specific.contains("punct", ignoreCase = true) -> CorrectionCategory.PUNCTUATION
        general.contains("casing", ignoreCase = true) || general.contains("capital", ignoreCase = true) -> CorrectionCategory.CAPITALIZATION
        general.contains("style", ignoreCase = true) -> CorrectionCategory.STYLE
        else -> CorrectionCategory.GRAMMAR
    }

    private fun mapErrorSeverity(general: String): CorrectionSeverity = when {
        general.contains("spell", ignoreCase = true) -> CorrectionSeverity.ERROR
        general.contains("style", ignoreCase = true) -> CorrectionSeverity.SUGGESTION
        else -> CorrectionSeverity.WARNING
    }

    private fun buildExplanation(errorType: String, generalType: String): String = when {
        generalType.isNotBlank() && errorType.isNotBlank() -> "$generalType: $errorType"
        generalType.isNotBlank() -> generalType
        errorType.isNotBlank() -> errorType
        else -> "Grammar correction"
    }
}
