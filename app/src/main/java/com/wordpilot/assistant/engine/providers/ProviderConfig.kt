package com.wordpilot.assistant.engine.providers

data class ProviderConfig(
    val id: String,
    val enabled: Boolean,
    val apiKey: String?,         // null = not configured
    val baseUrl: String?,        // for self-hosted (LanguageTool)
    val timeoutSeconds: Int = 10,
    val maxRequestChars: Int = 4000,
    val priority: Int            // lower = higher priority
) {
    val isOperational: Boolean
        get() = enabled && when (id) {
            "languagetool" -> !baseUrl.isNullOrBlank()
            "gector" -> true  // local, no key needed
            else -> !apiKey.isNullOrBlank()
        }
}
