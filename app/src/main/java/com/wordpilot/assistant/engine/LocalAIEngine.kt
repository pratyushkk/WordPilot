package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.ModelInfo
import com.wordpilot.assistant.domain.models.ModelState
import com.wordpilot.assistant.domain.models.ResourceUsage
import com.wordpilot.assistant.domain.models.RewriteResult
import com.wordpilot.assistant.domain.models.RewriteStyle
import kotlinx.coroutines.flow.StateFlow

interface LocalAIEngine {
    val modelState: StateFlow<ModelState>
    val isModelInstalled: Boolean
    val modelInfo: ModelInfo?

    suspend fun loadModel()
    suspend fun unloadModel()
    suspend fun rewrite(text: String, style: RewriteStyle, customInstruction: String? = null): RewriteResult
    suspend fun explainErrors(text: String, corrections: List<com.wordpilot.assistant.domain.models.Correction>): String
    fun cancelInference()
    fun getResourceUsage(): ResourceUsage
}
