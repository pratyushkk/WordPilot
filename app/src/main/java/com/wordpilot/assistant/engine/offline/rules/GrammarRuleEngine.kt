package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.morphology.MorphologyAnalyzer

class GrammarRuleEngine(
    private val morphology: MorphologyAnalyzer
) {

    private val rules: List<Rule> = listOf(
        SubjectVerbAgreementRule(morphology),
        ArticleRule(morphology),
        VerbFormRule(morphology),
        PronounRule(),
        DeterminerQuantifierRule(morphology),
        ConfusedWordsRule(),
        AdjectiveAdverbRule(),
        NegationRedundancyRule(),
        PunctuationChecker(),
        SentenceStructureChecker()
    )

    private var disabledRuleIds: Set<String> = emptySet()

    fun checkSentence(sentence: AnalyzedSentence): List<GrammarError> {
        val activeRules = rules.filter { it.id !in disabledRuleIds }
        val errors = mutableListOf<GrammarError>()

        for (rule in activeRules) {
            try {
                val ruleErrors = rule.evaluate(sentence)
                errors.addAll(ruleErrors)
            } catch (_: Exception) {
                // Rule failure isolation
            }
        }

        return errors
    }

    fun getAllRules(): List<Rule> = rules

    fun getSupportedRuleIds(): List<String> = rules.map { it.id }

    fun setDisabledRules(ruleIds: Set<String>) {
        disabledRuleIds = ruleIds
    }
}
