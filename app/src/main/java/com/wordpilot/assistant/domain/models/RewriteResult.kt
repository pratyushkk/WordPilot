package com.wordpilot.assistant.domain.models

data class RewriteResult(
    val originalText: String,
    val rewrittenText: String,
    val style: RewriteStyle,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

enum class RewriteStyle(val displayName: String, val prompt: String) {
    FIX_GRAMMAR("Fix Grammar", "Fix all grammar and spelling errors in the following text, preserving the original meaning:"),
    IMPROVE_CLARITY("Improve Clarity", "Rewrite the following text to be clearer and easier to understand:"),
    MAKE_PROFESSIONAL("Make Professional", "Rewrite the following text in a professional tone:"),
    MAKE_CASUAL("Make Casual", "Rewrite the following text in a casual, friendly tone:"),
    MAKE_POLITE("Make Polite", "Rewrite the following text to be more polite and courteous:"),
    MAKE_CONCISE("Make Concise", "Rewrite the following text to be more concise while preserving the meaning:"),
    IMPROVE_NATURAL("Improve Naturalness", "Rewrite the following text to sound more natural:"),
    CUSTOM("Custom", "")
}
