package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.RewriteResult
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.engine.gector.GectorOnnxEngine
import com.wordpilot.assistant.engine.impl.GeminiAIEngine
import com.wordpilot.assistant.engine.impl.RuleBasedRewriteEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RewriteCoordinator(
    private val geminiEngine: GeminiAIEngine,
    private val localEngine: LocalAIEngine,
    private val ruleEngine: RuleBasedRewriteEngine,
    private val gectorEngine: GectorOnnxEngine? = null,
    private val safeReplacementHelper: SafeReplacementHelper = SafeReplacementHelper(),
    initialEngineType: AIEngineType = AIEngineType.GECTOR
) {
    private val _selectedEngine = MutableStateFlow(initialEngineType)
    val selectedEngine: StateFlow<AIEngineType> = _selectedEngine.asStateFlow()

    @Volatile
    var isBlockCloudAi: Boolean = false
        private set

    fun setBlockCloudAi(blocked: Boolean) {
        isBlockCloudAi = blocked
        if (blocked && _selectedEngine.value == AIEngineType.GEMINI_FLASH) {
            _selectedEngine.value = AIEngineType.GECTOR
        }
    }

    fun setEngineType(type: AIEngineType) {
        if (isBlockCloudAi && type == AIEngineType.GEMINI_FLASH) {
            _selectedEngine.value = AIEngineType.GECTOR
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
            AIEngineType.GECTOR
        } else {
            targetEngine
        }

        return when (engineType) {
            AIEngineType.GECTOR -> {
                rewriteWithGector(text, style, customInstruction)
            }
            AIEngineType.GEMINI_FLASH -> {
                if (isBlockCloudAi) {
                    rewriteWithGector(text, style, customInstruction)
                } else if (!geminiEngine.isModelInstalled) {
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

    private suspend fun rewriteWithGector(
        text: String,
        style: RewriteStyle,
        customInstruction: String?
    ): RewriteResult {
        if (gectorEngine != null && gectorEngine.isAvailable()) {
            val corrections = gectorEngine.check(text)
            if (corrections.isNotEmpty()) {
                val fixed = safeReplacementHelper.applyMultipleCorrections(text, corrections)
                return RewriteResult(
                    originalText = text,
                    rewrittenText = if (fixed.isNotBlank()) fixed else text,
                    style = style,
                    isSuccess = true
                )
            }
        }

        // Return original text with no built-in rule fallbacks
        return RewriteResult(
            originalText = text,
            rewrittenText = text,
            style = style,
            isSuccess = true
        )
    }

    fun cancel() {
        geminiEngine.cancelInference()
        localEngine.cancelInference()
    }

    fun isEngineReady(type: AIEngineType): Boolean {
        return when (type) {
            AIEngineType.GECTOR -> gectorEngine?.isAvailable() == true || true
            AIEngineType.GEMINI_FLASH -> if (isBlockCloudAi) false else geminiEngine.isModelInstalled
            AIEngineType.LOCAL_AI -> localEngine.isModelInstalled
            AIEngineType.RULE_BASED -> true
        }
    }
}
