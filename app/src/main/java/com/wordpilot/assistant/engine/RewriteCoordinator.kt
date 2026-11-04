package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.RewriteResult
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.engine.impl.GeminiAIEngine
import com.wordpilot.assistant.engine.impl.RuleBasedRewriteEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RewriteCoordinator(
    private val geminiEngine: GeminiAIEngine,
    private val localEngine: LocalAIEngine,
    private val ruleEngine: RuleBasedRewriteEngine,
    private val safeReplacementHelper: SafeReplacementHelper = SafeReplacementHelper(),
    initialEngineType: AIEngineType = AIEngineType.GEMINI_FLASH
) {
    private val _selectedEngine = MutableStateFlow(initialEngineType)
    val selectedEngine: StateFlow<AIEngineType> = _selectedEngine.asStateFlow()

    @Volatile
    var isBlockCloudAi: Boolean = false
        private set

    fun setBlockCloudAi(blocked: Boolean) {
        isBlockCloudAi = blocked
        if (blocked && _selectedEngine.value == AIEngineType.GEMINI_FLASH) {
            _selectedEngine.value = AIEngineType.RULE_BASED
        }
    }

    fun setEngineType(type: AIEngineType) {
        if (isBlockCloudAi && type == AIEngineType.GEMINI_FLASH) {
            _selectedEngine.value = AIEngineType.RULE_BASED
        } else {
            _selectedEngine.value = type
        }
    }

    suspend fun rewrite(
        text: String,
        style: RewriteStyle,
        customInstruction: String? = null,
        forcedEngine: AIEngineType? = null
    ): RewriteResult {
        val targetEngine = forcedEngine ?: _selectedEngine.value
        val engineType = if (isBlockCloudAi && targetEngine == AIEngineType.GEMINI_FLASH) {
            AIEngineType.RULE_BASED
        } else {
            targetEngine
        }

        return when (engineType) {
            AIEngineType.GEMINI_FLASH -> {
                if (isBlockCloudAi) {
                    ruleEngine.rewrite(text, style, customInstruction)
                } else if (!geminiEngine.isModelInstalled) {
                    val offlineResult = ruleEngine.rewrite(text, style, customInstruction)
                    offlineResult.copy(
                        errorMessage = "Gemini API key is not configured. (Applied offline rules instead)"
                    )
                } else {
                    val result = geminiEngine.rewrite(text, style, customInstruction)
                    if (!result.isSuccess) {
                        val offlineFallback = ruleEngine.rewrite(text, style, customInstruction)
                        if (offlineFallback.isSuccess && offlineFallback.rewrittenText != text) {
                            offlineFallback.copy(
                                errorMessage = "${result.errorMessage} (Applied offline rules as fallback)"
                            )
                        } else {
                            result
                        }
                    } else {
                        result
                    }
                }
            }
            AIEngineType.LOCAL_AI -> {
                if (!localEngine.isModelInstalled) {
                    val offlineResult = ruleEngine.rewrite(text, style, customInstruction)
                    offlineResult.copy(
                        errorMessage = "Local AI model is not installed. (Applied offline rules instead)"
                    )
                } else {
                    localEngine.rewrite(text, style, customInstruction)
                }
            }
            AIEngineType.RULE_BASED -> {
                ruleEngine.rewrite(text, style, customInstruction)
            }
        }
    }

        val gemini: GeminiAIEngine get() = geminiEngine

    suspend fun summarize(text: String, mode: String = "BULLETS"): Result<String> =
        geminiEngine.summarize(text, mode)

    suspend fun generateSmartReplies(incomingContext: String): Result<List<String>> =
        geminiEngine.generateSmartReplies(incomingContext)

    suspend fun translate(text: String, targetLanguage: String): Result<String> =
        geminiEngine.translate(text, targetLanguage)

    suspend fun explain(text: String): Result<String> =
        geminiEngine.explain(text)

    fun cancel() {
        geminiEngine.cancelInference()
        localEngine.cancelInference()
    }

    fun isEngineReady(type: AIEngineType): Boolean {
        return when (type) {
            AIEngineType.GEMINI_FLASH -> if (isBlockCloudAi) false else geminiEngine.isModelInstalled
            AIEngineType.LOCAL_AI -> localEngine.isModelInstalled
            AIEngineType.RULE_BASED -> true
        }
    }
}
