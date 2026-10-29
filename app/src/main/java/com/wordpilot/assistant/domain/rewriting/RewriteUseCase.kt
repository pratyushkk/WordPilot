package com.wordpilot.assistant.domain.rewriting

import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.RewriteResult
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.engine.RewriteCoordinator

class RewriteUseCase(
    val coordinator: RewriteCoordinator
) {
    val isAvailable: Boolean
        get() = true

    suspend fun rewrite(
        text: String,
        style: RewriteStyle,
        customInstruction: String? = null,
        forcedEngine: AIEngineType? = null
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

        return try {
            coordinator.rewrite(text, style, customInstruction, forcedEngine)
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
        coordinator.cancel()
    }
}
