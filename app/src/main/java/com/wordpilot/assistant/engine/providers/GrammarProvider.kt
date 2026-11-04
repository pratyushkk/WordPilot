package com.wordpilot.assistant.engine.providers

interface GrammarProvider {
    val id: String
    fun configure(config: ProviderConfig)
    fun isConfigured(): Boolean
    fun isEnabled(): Boolean
    suspend fun check(text: String, language: String = "en"): ProviderResult
}
