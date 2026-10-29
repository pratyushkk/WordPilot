package com.wordpilot.assistant.domain.rewriting

import com.wordpilot.assistant.domain.models.RewriteResult
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.engine.LocalAIEngine

class RewriteUseCase(
    private val aiEngine: LocalAIEngine
) {
    val isAvailable: Boolean
        get() = aiEngine.isModelInstalled

    suspend fun rewrite(
        text: String,
        style: RewriteStyle,
        customInstruction: String? = null
    ): RewriteResult {
        if (text.isBlank()) {
            return RewriteResult(
                originalText = text,
                rewrittenText = text,
                style = style,
                isSuccess = false,
                errorMessage = "No text to rewrite"
            )
        }

        if (!aiEngine.isModelInstalled) {
            return RewriteResult(
                originalText = text,
                rewrittenText = text,
                style = style,
                isSuccess = false,
                errorMessage = "AI model not installed. Install it from Settings → AI Model."
            )
        }

        return try {
            aiEngine.rewrite(text, style, customInstruction)
        } catch (e: Exception) {
            RewriteResult(
                originalText = text,
                rewrittenText = text,
                style = style,
                isSuccess = false,
                errorMessage = e.message ?: "Rewrite failed"
            )
        }
    }

    fun cancel() {
        aiEngine.cancelInference()
    }
}
