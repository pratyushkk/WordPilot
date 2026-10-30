package com.wordpilot.assistant.engine

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
    initialEngineType: AIEngineType = AIEngineType.GEMINI_FLASH
) {
    private val _selectedEngine = MutableStateFlow(initialEngineType)
    val selectedEngine: StateFlow<AIEngineType> = _selectedEngine.asStateFlow()

    fun setEngineType(type: AIEngineType) {
        _selectedEngine.value = type
    }

    suspend fun rewrite(
        text: String,
        style: RewriteStyle,
        customInstruction: String? = null,
        forcedEngine: AIEngineType? = null
    ): RewriteResult {
        val engineType = forcedEngine ?: _selectedEngine.value

        return when (engineType) {
            AIEngineType.GEMINI_FLASH -> {
                if (!geminiEngine.isModelInstalled) {
                    // Fall back to rule-based engine if user has not configured API key yet
                    val offlineResult = ruleEngine.rewrite(text, style, customInstruction)
                    offlineResult.copy(
                        errorMessage = "Gemini API key is not configured. (Applied offline grammar rules instead)"
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

    fun cancel() {
        geminiEngine.cancelInference()
        localEngine.cancelInference()
    }

    fun isEngineReady(type: AIEngineType): Boolean {
        return when (type) {
            AIEngineType.GEMINI_FLASH -> geminiEngine.isModelInstalled
            AIEngineType.LOCAL_AI -> localEngine.isModelInstalled
            AIEngineType.RULE_BASED -> true
        }
    }
}
