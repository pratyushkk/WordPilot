package com.wordpilot.assistant.domain.models

enum class AIEngineType(
    val id: String,
    val displayName: String,
    val shortName: String,
    val badge: String
) {
    GEMINI_FLASH("gemini", "Gemini 1.5 Flash (Cloud)", "Gemini Flash", "Cloud AI"),
    LOCAL_AI("local", "Local AI Model (Llama 3.2)", "Local AI", "On-Device"),
    RULE_BASED("rule_based", "Offline Rules", "Offline Rules", "Offline");

    companion object {
        fun fromId(id: String?): AIEngineType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: GEMINI_FLASH
        }
    }
}
