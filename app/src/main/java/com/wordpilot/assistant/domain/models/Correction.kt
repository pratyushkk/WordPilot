package com.wordpilot.assistant.domain.models

import java.util.UUID

data class Correction(
    val id: String = UUID.randomUUID().toString(),
    val startOffset: Int,
    val endOffset: Int,
    val originalText: String,
    val suggestedReplacement: String?,
    val category: CorrectionCategory,
    val explanation: String = "",
    val severity: CorrectionSeverity,
    val confidence: Float = 1.0f,
    val ruleId: String,
    val isSafeToApply: Boolean = true
)

enum class CorrectionCategory {
    SPELLING, GRAMMAR, PUNCTUATION, STYLE, CAPITALIZATION;

    fun displayName(): String = when (this) {
        SPELLING -> "Spelling"
        GRAMMAR -> "Grammar"
        PUNCTUATION -> "Punctuation"
        STYLE -> "Style"
        CAPITALIZATION -> "Capitalization"
    }
}

enum class CorrectionSeverity {
    ERROR, WARNING, SUGGESTION
}
