package com.wordpilot.assistant.engine.offline.test

import com.wordpilot.assistant.engine.offline.OfflineGrammarEngineFacade

data class RuleTestResult(
    val ruleId: String,
    val positiveTotal: Int,
    val positivePassed: Int,
    val negativeTotal: Int,
    val negativePassed: Int,
    val failures: List<String>
) {
    val isPassed: Boolean get() = positivePassed == positiveTotal && negativePassed == negativeTotal
}

data class TestSuiteReport(
    val totalRules: Int,
    val rulesPassed: Int,
    val totalTestCases: Int,
    val totalPassedTestCases: Int,
    val results: List<RuleTestResult>
) {
    val overallAccuracy: Float get() = if (totalTestCases == 0) 1.0f else totalPassedTestCases.toFloat() / totalTestCases
}

class GrammarTestRunner(
    private val facade: OfflineGrammarEngineFacade = OfflineGrammarEngineFacade(null)
) {

    suspend fun runAllTests(): TestSuiteReport {
        val rules = facade.ruleEngine.getAllRules()
        val results = mutableListOf<RuleTestResult>()
        var totalTestCases = 0
        var totalPassedTestCases = 0
        var rulesPassedCount = 0

        for (rule in rules) {
            var posPassed = 0
            var negPassed = 0
            val failures = mutableListOf<String>()

            // Positive tests (Must detect error)
            for (example in rule.positiveExamples) {
                totalTestCases++
                val corrections = facade.checkText(example, minConfidence = 0.50f)
                val matched = corrections.any { rule.isResponsibleFor(it.ruleId) }
                if (matched) {
                    posPassed++
                    totalPassedTestCases++
                } else {
                    failures.add("FAIL POSITIVE: \"$example\" was not flagged by ${rule.id} (found: ${corrections.map { it.ruleId }})")
                }
            }

            // Negative tests (Must NOT detect error)
            for (example in rule.negativeExamples) {
                totalTestCases++
                val corrections = facade.checkText(example, minConfidence = 0.50f)
                val falsePositive = corrections.any { rule.isResponsibleFor(it.ruleId) }
                if (!falsePositive) {
                    negPassed++
                    totalPassedTestCases++
                } else {
                    val flagged = corrections.filter { rule.isResponsibleFor(it.ruleId) }
                    failures.add("FAIL NEGATIVE: Valid sentence \"$example\" incorrectly flagged by ${rule.id}: ${flagged.map { "${it.originalText} -> ${it.suggestedReplacement}" }}")
                }
            }

            val rResult = RuleTestResult(
                ruleId = rule.id,
                positiveTotal = rule.positiveExamples.size,
                positivePassed = posPassed,
                negativeTotal = rule.negativeExamples.size,
                negativePassed = negPassed,
                failures = failures
            )
            if (rResult.isPassed) rulesPassedCount++
            results.add(rResult)
        }

        return TestSuiteReport(
            totalRules = rules.size,
            rulesPassed = rulesPassedCount,
            totalTestCases = totalTestCases,
            totalPassedTestCases = totalPassedTestCases,
            results = results
        )
    }
}
