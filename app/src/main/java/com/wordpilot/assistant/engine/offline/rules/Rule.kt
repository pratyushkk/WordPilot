package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError

interface Rule {
    val id: String
    val category: CorrectionCategory
    val severity: CorrectionSeverity
    val confidence: Float
    val description: String
    val positiveExamples: List<String>
    val negativeExamples: List<String>

    fun evaluate(sentence: AnalyzedSentence): List<GrammarError>

    fun isResponsibleFor(errorRuleId: String): Boolean = errorRuleId == id || errorRuleId.startsWith(id)
}
