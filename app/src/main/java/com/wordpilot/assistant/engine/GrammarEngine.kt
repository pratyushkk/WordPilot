package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.Correction

interface GrammarEngine {
    suspend fun check(text: String): List<Correction>
    fun isAvailable(): Boolean
    fun getSupportedRuleIds(): List<String>
    fun setDisabledRules(ruleIds: Set<String>)
}
