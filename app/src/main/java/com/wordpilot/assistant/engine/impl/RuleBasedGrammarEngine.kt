package com.wordpilot.assistant.engine.impl

import android.content.Context
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.engine.GrammarEngine
import com.wordpilot.assistant.engine.SentenceSpan
import com.wordpilot.assistant.engine.offline.OfflineGrammarEngineFacade
import com.wordpilot.assistant.engine.offline.model.AnalysisOptions

class RuleBasedGrammarEngine(
    context: Context? = null,
    val facade: OfflineGrammarEngineFacade = OfflineGrammarEngineFacade(context)
) : GrammarEngine {

    override suspend fun check(text: String): List<Correction> {
        if (text.isBlank()) return emptyList()
        return facade.analyze(
            rawText = text,
            options = AnalysisOptions(includeSpelling = false, includeGrammar = true)
        )
    }

    override fun isAvailable(): Boolean = true

    override fun getSupportedRuleIds(): List<String> = facade.getSupportedRuleIds()

    override fun setDisabledRules(ruleIds: Set<String>) {
        facade.setDisabledRules(ruleIds)
    }
}
