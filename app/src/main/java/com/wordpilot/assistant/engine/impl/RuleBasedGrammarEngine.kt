package com.wordpilot.assistant.engine.impl

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.engine.GrammarEngine
import com.wordpilot.assistant.engine.SentenceAnalyzer
import com.wordpilot.assistant.engine.rules.ArticleRule
import com.wordpilot.assistant.engine.rules.CapitalizationRule
import com.wordpilot.assistant.engine.rules.ConfusedWordsRule
import com.wordpilot.assistant.engine.rules.GrammarRule
import com.wordpilot.assistant.engine.rules.PunctuationSpacingRule
import com.wordpilot.assistant.engine.rules.RepeatedWordRule
import com.wordpilot.assistant.engine.rules.SubjectVerbAgreementRule

class RuleBasedGrammarEngine : GrammarEngine {
    
    private val sentenceAnalyzer = SentenceAnalyzer()
    
    private val allRules: List<GrammarRule> = listOf(
        RepeatedWordRule(),
        CapitalizationRule(),
        PunctuationSpacingRule(),
        ArticleRule(),
        SubjectVerbAgreementRule(),
        ConfusedWordsRule()
    )

    private var disabledRules: Set<String> = emptySet()

    override suspend fun check(text: String): List<Correction> {
        if (text.isBlank()) return emptyList()

        val sentences = sentenceAnalyzer.splitSentences(text)
        val activeRules = allRules.filter { it.id !in disabledRules }

        return activeRules.flatMap { rule ->
            try {
                rule.check(text, sentences)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun isAvailable(): Boolean = true

    override fun getSupportedRuleIds(): List<String> = allRules.map { it.id }

    override fun setDisabledRules(ruleIds: Set<String>) {
        disabledRules = ruleIds
    }
}
