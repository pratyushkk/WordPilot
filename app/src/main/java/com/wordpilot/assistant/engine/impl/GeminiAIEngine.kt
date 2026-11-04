package com.wordpilot.assistant.engine.impl

import android.util.Log
import com.wordpilot.assistant.domain.models.*
import com.wordpilot.assistant.engine.LocalAIEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiAIEngine(
    private val apiKeyProvider: () -> String?
) : LocalAIEngine {

    companion object {
        private const val TAG = "GeminiAIEngine"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

        /**
         * Sanitizes user-pasted API keys to eliminate whitespace, accidental wrapping quotes,
         * carriage returns, or prefix keywords (like 'Bearer ' or 'key=').
         */
        fun sanitizeApiKey(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return raw.trim()
                .removePrefix("\"").removeSuffix("\"")
                .removePrefix("'").removeSuffix("'")
                .removePrefix("Bearer ").removePrefix("bearer ")
                .removePrefix("key=").removePrefix("KEY=")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()
        }
    }

    private val _modelState = MutableStateFlow(ModelState.READY)
    override val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    override val isModelInstalled: Boolean
        get() = sanitizeApiKey(apiKeyProvider()).isNotBlank()

    override val modelInfo: ModelInfo = ModelInfo(
        name = "Gemini Model",
        source = "Google AI Studio",
        license = "Google Cloud",
        downloadSizeMb = 0,
        installedSizeMb = 0,
        isInstalled = true
    )

    private var activeConnection: HttpURLConnection? = null

    override suspend fun loadModel() {
        _modelState.value = ModelState.READY
    }

    override suspend fun unloadModel() {
        cancelInference()
        _modelState.value = ModelState.READY
    }

    override suspend fun rewrite(
        text: String,
        style: RewriteStyle,
        customInstruction: String?
    ): RewriteResult = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKeyProvider())
        if (cleanKey.isBlank()) {
            return@withContext RewriteResult(
                originalText = text,
                rewrittenText = text,
                style = style,
                isSuccess = false,
                errorMessage = "Gemini API key is not configured. Tap 'Rewrite & Tone' to add your API key."
            )
        }

        if (text.isBlank()) {
            return@withContext RewriteResult(
                originalText = text,
                rewrittenText = text,
                style = style,
                isSuccess = false,
                errorMessage = "Text is empty."
            )
        }

        _modelState.value = ModelState.LOADING
        val prompt = buildPrompt(text, style, customInstruction)

        try {
            val url = URL("$BASE_URL?key=$cleanKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                doInput = true
            }
            activeConnection = connection

            // Construct JSON request body matching Gemini REST API specification
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1024)
                }
                put("generationConfig", generationConfig)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = BufferedReader(InputStreamReader(responseStream, "UTF-8")).use { reader ->
                reader.readText()
            }

            if (responseCode !in 200..299) {
                Log.e(TAG, "Gemini API HTTP $responseCode: $responseBody")
                val errorMsg = parseErrorMessage(responseBody, responseCode)
                _modelState.value = ModelState.READY
                return@withContext RewriteResult(
                    originalText = text,
                    rewrittenText = text,
                    style = style,
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }

            val parsedOutput = parseGeminiOutput(responseBody)
            val cleanedOutput = cleanRewrittenText(parsedOutput, text)

            _modelState.value = ModelState.READY
            return@withContext RewriteResult(
                originalText = text,
                rewrittenText = cleanedOutput,
                style = style,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini rewrite failed", e)
            _modelState.value = ModelState.READY
            return@withContext RewriteResult(
                originalText = text,
                rewrittenText = text,
                style = style,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to connect to Gemini."
            )
        } finally {
            activeConnection?.disconnect()
            activeConnection = null
        }
    }

    override suspend fun explainErrors(
        text: String,
        corrections: List<com.wordpilot.assistant.domain.models.Correction>
    ): String = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKeyProvider())
        if (cleanKey.isBlank()) {
            return@withContext "Gemini API key is not configured."
        }
        val prompt = "Briefly explain the grammar improvements for this text:\n$text"
        val result = rewrite(prompt, RewriteStyle.REWRITE)
        if (result.isSuccess) result.rewrittenText else "No explanation available."
    }

    override fun cancelInference() {
        try {
            activeConnection?.disconnect()
            activeConnection = null
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling Gemini inference", e)
        }
        _modelState.value = ModelState.READY
    }

    override fun getResourceUsage(): ResourceUsage {
        return ResourceUsage(
            memoryUsageMb = 0,
            isModelLoaded = true,
            loadTimeMs = 0,
            lastInferenceTimeMs = 0
        )
    }

    fun buildPrompt(text: String, style: RewriteStyle, customInstruction: String?): String {
        val instruction = when (style) {
            RewriteStyle.FIX_GRAMMAR ->
                "Fix all grammatical, spelling, capitalization, and punctuation mistakes. Preserve the original meaning and tone strictly."
            RewriteStyle.REWRITE ->
                "Rewrite the text to be fluent, natural, and expressive while keeping the core meaning."
            RewriteStyle.IMPROVE_CLARITY ->
                "Rewrite the text to be crystal clear, crisp, and unambiguous."
            RewriteStyle.MAKE_PROFESSIONAL ->
                "Rewrite the text in a polished, respectful, executive business tone suitable for work or email."
            RewriteStyle.MAKE_CONCISE ->
                "Rewrite the text to be concise and direct. Cut filler and wordiness while keeping every essential point."
            RewriteStyle.MAKE_POLITE ->
                "Rewrite the text to be courteous, friendly, warm, and polite."
            RewriteStyle.MAKE_CASUAL ->
                "Rewrite the text in a relaxed, modern, casual conversational tone."
            RewriteStyle.IMPROVE_NATURAL ->
                "Rewrite the text to sound like a native English speaker with natural phrasing."
            RewriteStyle.EXPLAIN_MISTAKES ->
                "Explain the mistakes found in this text."
            RewriteStyle.CUSTOM ->
                customInstruction?.takeIf { it.isNotBlank() } ?: "Improve this text."
        }

        return """
            You are WordPilot, a premier mobile writing assistant.
            Instruction: $instruction
            
            Original Text:
            "$text"
            
            CRITICAL RULES:
            1. Output ONLY the rewritten text.
            2. Do NOT add any preamble, conversational commentary, or explanations.
            3. Do NOT surround the output in quotation marks or markdown backticks.
        """.trimIndent()
    }

    fun parseGeminiOutput(responseJson: String): String {
        try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            return parts.getJSONObject(0).optString("text", "")
        } catch (_: Exception) {
            return ""
        }
    }

    fun cleanRewrittenText(raw: String, originalText: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length >= 2) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }
        if (cleaned.startsWith("“") && cleaned.endsWith("”") && cleaned.length >= 2) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }
        if (cleaned.startsWith("```") && cleaned.endsWith("```") && cleaned.length >= 6) {
            cleaned = cleaned.substring(3, cleaned.length - 3).trim()
        }
        return if (cleaned.isNotBlank()) cleaned else originalText
    }

    fun parseErrorMessage(responseBody: String, code: Int): String {
        try {
            val root = JSONObject(responseBody)
            val error = root.optJSONObject("error")
            if (error != null) {
                val message = error.optString("message", "")
                val status = error.optString("status", "")

                // Check error details for API_KEY_INVALID specifically
                val details = error.optJSONArray("details")
                var isKeyInvalid = false
                if (details != null) {
                    for (i in 0 until details.length()) {
                        val detail = details.optJSONObject(i)
                        if (detail?.optString("reason") == "API_KEY_INVALID") {
                            isKeyInvalid = true
                            break
                        }
                    }
                }

                if (isKeyInvalid || message.contains("API key not valid", ignoreCase = true) || message.contains("API_KEY_INVALID", ignoreCase = true)) {
                    return "Invalid Gemini API key. Please check your key from Google AI Studio (aistudio.google.com/app/apikey)."
                }
                if (status == "PERMISSION_DENIED" || code == 403) {
                    return "Permission denied (HTTP 403). Make sure the Generative Language API is enabled for this API key in Google Cloud/AI Studio."
                }
                if (status == "RESOURCE_EXHAUSTED" || code == 429) {
                    return "Gemini API quota exceeded (HTTP 429). Please wait a moment."
                }
                if (message.isNotBlank()) {
                    return "Gemini error: $message"
                }
            }
        } catch (_: Exception) {}
        return "Gemini request failed (HTTP $code). Verify your API key or check your internet connection."
    }
}
