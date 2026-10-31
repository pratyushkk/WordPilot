package com.wordpilot.assistant.domain.models

enum class AIEngineType(
    val id: String,
    val displayName: String,
    val shortName: String,
    val badge: String
) {
    GECTOR("gector", "GECToR Neural Engine (On-Device)", "GECToR", "On-Device Neural"),
    RULE_BASED("rule_based", "Offline Grammar Rules", "Offline Rules", "Offline"),
    LOCAL_AI("local", "Local AI Model (Llama 3.2)", "Local AI", "On-Device"),
    GEMINI_FLASH("gemini", "Gemini 1.5 Flash (Cloud)", "Gemini Flash", "Cloud AI");

    companion object {
        fun fromId(id: String?): AIEngineType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: GECTOR
        }
    }
}
