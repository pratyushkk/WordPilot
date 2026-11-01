package com.wordpilot.assistant.engine.providers

import android.util.Log
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.normalization.OffsetValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Self-hosted LanguageTool provider integration.
 * Connects to a self-hosted instance (e.g. Docker / server).
 * Never points to public unauthenticated free endpoints without explicit configuration.
 */
class LanguageToolProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : GrammarProvider {
    override val id = "languagetool"

    companion object {
        private const val TAG = "LanguageToolProvider"
        private const val PATH = "/v2/check"
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
                return@withContext ProviderResult.Failure.Disabled(id, "LanguageTool provider is disabled")
            }
            val baseUrl = cfg.baseUrl
            if (baseUrl.isNullOrBlank()) {
                return@withContext ProviderResult.Failure.NotConfigured(
                    id, "LanguageTool base URL not configured. Please set a self-hosted instance URL."
                )
            }

            val startTime = System.currentTimeMillis()

            try {
                val endpointUrl = baseUrl.trimEnd('/') + PATH
                val formBody = FormBody.Builder()
                    .add("text", text)
                    .add("language", language)
                    .add("disabledRules", "WHITESPACE_RULE,COMMA_PARENTHESIS_WHITESPACE")
                    .build()

                val request = Request.Builder()
                    .url(endpointUrl)
                    .post(formBody)
                    .build()

                val response = client.newCall(request).execute()
                val statusCode = response.code
                val responseBody = response.body?.string().orEmpty()

                if (statusCode == 429) {
                    val retryAfter = response.header("Retry-After")?.toLongOrNull()
                    return@withContext ProviderResult.Failure.RateLimited(id, "LanguageTool rate limit reached", retryAfter)
                }

                if (!response.isSuccessful) {
                    Log.e(TAG, "LanguageTool HTTP $statusCode: $responseBody")
                    return@withContext ProviderResult.Failure.HttpError(id, "LanguageTool returned HTTP $statusCode", statusCode)
                }

                parseResponse(responseBody, text, System.currentTimeMillis() - startTime)
            } catch (e: java.net.SocketTimeoutException) {
                ProviderResult.Failure.Timeout(id, "LanguageTool request timed out")
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to LanguageTool", e)
                ProviderResult.Failure.NetworkError(id, "LanguageTool error: ${e.javaClass.simpleName}", e)
            }
        }

    private fun parseResponse(responseBody: String, originalText: String, durationMs: Long): ProviderResult {
        return try {
            val root = JSONObject(responseBody)
            val matches = root.optJSONArray("matches") ?: return ProviderResult.Success(emptyList(), id, processingTimeMs = durationMs)
            val corrections = mutableListOf<Correction>()

            for (i in 0 until matches.length()) {
                val match = matches.getJSONObject(i)
                val offset = match.optInt("offset", -1)
                val length = match.optInt("length", -1)
                if (offset < 0 || length < 0) continue

                val end = offset + length
                val validated = OffsetValidator.validate(originalText, offset, end) ?: continue
                val originalSpan = try {
                    originalText.substring(validated.first, validated.second)
                } catch (e: Exception) {
                    continue
                }

                val replacements = match.optJSONArray("replacements")
                val replacement = if (replacements != null && replacements.length() > 0) {
                    replacements.getJSONObject(0).optString("value", "")
                } else ""

                val rule = match.optJSONObject("rule")
                val ruleId = rule?.optString("id", "LT_RULE") ?: "LT_RULE"
                val ruleCategory = rule?.optJSONObject("category")?.optString("id", "") ?: ""
                val message = match.optString("message", "Grammar suggestion")
                val shortMessage = match.optString("shortMessage", "")

                val category = mapCategory(ruleCategory, ruleId)
                val severity = mapSeverity(ruleCategory, ruleId)

                corrections.add(
                    Correction(
                        startOffset = validated.first,
                        endOffset = validated.second,
                        originalText = originalSpan,
                        suggestedReplacement = replacement.ifBlank { null },
                        category = category,
                        explanation = shortMessage.ifBlank { message },
                        severity = severity,
                        confidence = -1f,
                        ruleId = "LT_$ruleId",
                        isSafeToApply = replacement.isNotBlank()
                    )
                )
            }

            ProviderResult.Success(corrections = corrections, providerId = id, processingTimeMs = durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse LanguageTool JSON", e)
            ProviderResult.Failure.InvalidResponse(id, "Failed to parse LanguageTool response: ${e.message}")
        }
    }

    private fun mapCategory(ruleCategory: String, ruleId: String): CorrectionCategory = when {
        ruleCategory.contains("TYPO", ignoreCase = true) || ruleId.contains("SPELL", ignoreCase = true) -> CorrectionCategory.SPELLING
        ruleCategory.contains("PUNCT", ignoreCase = true) -> CorrectionCategory.PUNCTUATION
        ruleCategory.contains("CASING", ignoreCase = true) -> CorrectionCategory.CAPITALIZATION
        ruleCategory.contains("STYLE", ignoreCase = true) -> CorrectionCategory.STYLE
        else -> CorrectionCategory.GRAMMAR
    }

    private fun mapSeverity(ruleCategory: String, ruleId: String): CorrectionSeverity = when {
        ruleCategory.contains("TYPO", ignoreCase = true) || ruleId.contains("SPELL", ignoreCase = true) -> CorrectionSeverity.ERROR
        ruleCategory.contains("STYLE", ignoreCase = true) -> CorrectionSeverity.SUGGESTION
        else -> CorrectionSeverity.WARNING
    }
}
