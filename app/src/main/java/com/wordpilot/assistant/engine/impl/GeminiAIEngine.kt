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

data class GeminiChatMessage(
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiAIEngine(
    private val apiKeyProvider: () -> String?
) : LocalAIEngine {

    companion object {
        private const val TAG = "GeminiAIEngine"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
    }

    private val _modelState = MutableStateFlow(ModelState.READY)
    override val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    override val isModelInstalled: Boolean
        get() = !apiKeyProvider().isNullOrBlank()

    override val modelInfo: ModelInfo = ModelInfo(
        name = "Google Gemini 1.5 Flash",
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
        // Cloud-based, no-op
    }

    override suspend fun rewrite(
        text: String,
        style: RewriteStyle,
        customInstruction: String?
    ): RewriteResult = withContext(Dispatchers.IO) {
        val rawApiKey = apiKeyProvider()?.trim()
        if (rawApiKey.isNullOrBlank()) {
            return@withContext RewriteResult(
                originalText = text,
                rewrittenText = text,
                style = style,
                isSuccess = false,
                errorMessage = "Gemini API key is not configured. Please enter your Gemini API key or switch to Offline mode."
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
            val url = URL("$BASE_URL?key=$rawApiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-goog-api-key", rawApiKey)
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                doInput = true
            }
            activeConnection = connection

            // Construct JSON request body
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
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
                    put("maxOutputTokens", 512)
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
                errorMessage = "Network or API error: ${e.localizedMessage ?: "Connection failed"}"
            )
        } finally {
            activeConnection = null
        }
    }

    override suspend fun explainErrors(
        text: String,
        corrections: List<Correction>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return@withContext "Configure your Gemini API key in Settings to get AI grammar explanations."
        }

        val prompt = "You are WordPilot, an expert writing tutor. Briefly explain the grammatical or spelling errors in this text in 2-3 concise bullet points:\n\n\"$text\""

        try {
            val url = URL("$BASE_URL?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 12000
                readTimeout = 15000
                doOutput = true
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(requestJson.toString()) }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                return@withContext parseGeminiOutput(body)
            } else {
                return@withContext "Unable to retrieve explanation (HTTP $responseCode)."
            }
        } catch (e: Exception) {
            return@withContext "Explanation unavailable: ${e.localizedMessage}"
        }
    }

    
    suspend fun executeDirectPrompt(
        prompt: String,
        temperature: Float = 0.2f,
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        val rawApiKey = apiKeyProvider()?.trim()
        if (rawApiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        try {
            val url = URL("$BASE_URL?key=$rawApiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-goog-api-key", rawApiKey)
                connectTimeout = 15000
                readTimeout = 25000
                doOutput = true
                doInput = true
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("temperature", temperature.toDouble())
                    put("maxOutputTokens", maxTokens)
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

            val responseBody = BufferedReader(InputStreamReader(responseStream, "UTF-8")).use { it.readText() }
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception(parseErrorMessage(responseBody, responseCode)))
            }

            val output = parseGeminiOutput(responseBody)
            Result.success(output)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatStream(
        message: String,
        history: List<GeminiChatMessage>,
        activeContextText: String?,
        onChunkReceived: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()?.trim()
        if (apiKey.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                onError("Gemini API key is not configured. Please add it in Settings.")
            }
            return@withContext
        }

        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:streamGenerateContent?alt=sse&key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("x-goog-api-key", apiKey)
                doOutput = true
                connectTimeout = 12000
                readTimeout = 30000
            }

            val contentsArray = JSONArray()
            val systemInstruction = "You are WordPilot, an elite floating AI writing assistant on Android. " +
                "The user is typing in an app. Context of active text: '$activeContextText'. " +
                "Be direct, concise, insightful, and helpful. Format in clean markdown."

            for (msg in history.takeLast(6)) {
                val role = if (msg.role == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                })
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", message)))
            })

            val requestBody = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1024)
                })
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(requestBody.toString()) }

            val fullResponse = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (currentLine.startsWith("data: ")) {
                        val jsonStr = currentLine.substring(6).trim()
                        if (jsonStr.isNotEmpty()) {
                            try {
                                val candidate = JSONObject(jsonStr)
                                    .getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text")
                                fullResponse.append(candidate)
                                withContext(Dispatchers.Main) {
                                    onChunkReceived(candidate)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onComplete(fullResponse.toString())
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Gemini Chat failed: ${e.localizedMessage}")
            }
        }
    }

    suspend fun summarize(
        text: String,
        mode: String = "BULLETS"
    ): Result<String> {
        val prompt = when (mode) {
            "ONE_SENTENCE" -> "Summarize the following text in exactly one clear, impactful sentence:\n\n$text"
            "KEY_TAKEAWAYS" -> "Extract the top 3 key takeaways from this text with bullet points:\n\n$text"
            else -> "Provide a concise bulleted summary of the following text, highlighting core facts:\n\n$text"
        }
        return executeDirectPrompt(prompt, temperature = 0.2f)
    }

    suspend fun generateSmartReplies(
        incomingContext: String
    ): Result<List<String>> {
        val prompt = """
        The user received the following message in a chat app:
        "$incomingContext"

        Generate exactly 3 smart, natural, contextual replies:
        1: Short agreement/affirmative
        2: Friendly elaboration or follow-up question
        3: Polite decline or alternative proposal

        Output ONLY a valid JSON array of 3 strings, e.g. ["Yes, sounds great!", "I'd love to, what time?", "Sorry, I'm tied up today."]. Do not include markdown or extra text.
        """.trimIndent()

        val rawResult = executeDirectPrompt(prompt, temperature = 0.4f)
        return rawResult.map { jsonString ->
            try {
                val cleaned = jsonString.replace("`json", "").replace("`", "").trim()
                val arr = JSONArray(cleaned)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                list
            } catch (e: Exception) {
                listOf("Sounds good!", "Thanks for letting me know.", "Let me get back to you soon.")
            }
        }
    }

    suspend fun translate(
        text: String,
        targetLanguage: String
    ): Result<String> {
        val prompt = "Translate the following text accurately into $targetLanguage. Output ONLY the translated text without commentary:\n\n$text"
        return executeDirectPrompt(prompt, temperature = 0.1f)
    }

    suspend fun explain(
        text: String
    ): Result<String> {
        val prompt = "Explain the meaning, context, and key terms in the following text simply and clearly in 2-3 sentences:\n\n$text"
        return executeDirectPrompt(prompt, temperature = 0.3f)
    }

    override fun cancelInference() {
        try {
            activeConnection?.disconnect()
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun getResourceUsage(): ResourceUsage {
        return ResourceUsage(
            memoryUsageMb = 0,
            isModelLoaded = true,
            loadTimeMs = 0,
            lastInferenceTimeMs = 0
        )
    }

    private fun buildPrompt(text: String, style: RewriteStyle, customInstruction: String?): String {
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
            3. Do NOT surround the output in markdown quotation marks or backticks.
        """.trimIndent()
    }

    private fun parseGeminiOutput(responseJson: String): String {
        val root = JSONObject(responseJson)
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""
        return parts.getJSONObject(0).optString("text", "")
    }

    private fun cleanRewrittenText(raw: String, originalText: String): String {
        var cleaned = raw.trim()
        // Strip surrounding quotation marks if LLM added them
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length >= 2) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }
        if (cleaned.startsWith("“") && cleaned.endsWith("”") && cleaned.length >= 2) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }
        return if (cleaned.isNotBlank()) cleaned else originalText
    }

    private fun parseErrorMessage(responseBody: String, code: Int): String {
        try {
            val root = JSONObject(responseBody)
            val error = root.optJSONObject("error")
            if (error != null) {
                val message = error.optString("message", "")
                val status = error.optString("status", "")
                if (message.contains("API_KEY_INVALID", ignoreCase = true) || status == "INVALID_ARGUMENT" || code == 400) {
                    return "Invalid Gemini API key. Please verify your key from Google AI Studio (aistudio.google.com/app/apikey). Valid keys usually start with 'AIzaSy'."
                }
                if (status == "PERMISSION_DENIED" || code == 403) {
                    return "Permission denied (HTTP 403). Make sure the Generative Language API is enabled for this API key in Google Cloud/AI Studio."
                }
                if (status == "RESOURCE_EXHAUSTED" || code == 429) {
                    return "Gemini API quota exceeded (HTTP 429). Please wait a moment."
                }
                if (message.isNotBlank()) return message
            }
        } catch (e: Exception) {
            // fallback
        }
        return "Gemini request failed (HTTP $code). Verify your API key from aistudio.google.com or check your internet connection."
    }
}
