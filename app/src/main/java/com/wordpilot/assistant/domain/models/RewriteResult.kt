package com.wordpilot.assistant.domain.models

data class RewriteResult(
    val originalText: String,
    val rewrittenText: String,
    val style: RewriteStyle,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

enum class RewriteStyle(val displayName: String, val prompt: String) {
    FIX_GRAMMAR("Fix Grammar", "Fix all grammar, punctuation, and spelling errors in the following text, preserving the original meaning exactly:"),
    REWRITE("Rewrite", "Rewrite the following text to sound fluent, polished, and natural while preserving the original meaning:"),
    IMPROVE_CLARITY("Improve Clarity", "Rewrite the following text to be clearer, crisper, and easier to understand:"),
    MAKE_PROFESSIONAL("Professional", "Rewrite the following text in an executive, polite, and professional business tone:"),
    MAKE_CONCISE("Make Concise", "Rewrite the following text to be concise and direct, cutting fluff while preserving the full meaning:"),
    MAKE_POLITE("Make Polite", "Rewrite the following text to be courteous, respectful, and polite:"),
    MAKE_CASUAL("Make Casual", "Rewrite the following text in a warm, friendly, and casual conversational tone:"),
    IMPROVE_NATURAL("Improve Naturalness", "Rewrite the following text to sound fluent and native:"),
    EXPLAIN_MISTAKES("Explain Mistakes", "Explain the grammatical, spelling, and structural mistakes in the text:"),
    CUSTOM("Custom", "")
}
