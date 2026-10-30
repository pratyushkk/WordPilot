package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.morphology.MorphologyAnalyzer
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class ArticleRule(
    private val morphology: MorphologyAnalyzer
) : Rule {

    override val id = "ARTICLE_A_AN"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.ERROR
    override val confidence = 0.98f
    override val description = "Use 'a' before consonant sounds and 'an' before vowel sounds."

    override val positiveExamples = listOf(
        "I saw a elephant at the zoo.",
        "We waited for a hour.",
        "She bought an car yesterday.",
        "He attends an university in Boston.",
        "They gave me an advice."
    )

    override val negativeExamples = listOf(
        "I saw an elephant at the zoo.",
        "We waited for an hour.",
        "She bought a car yesterday.",
        "He attends a university in Boston.",
        "They gave me advice."
    )

    override fun isResponsibleFor(errorRuleId: String): Boolean =
        super.isResponsibleFor(errorRuleId) || errorRuleId == "SUPERFLUOUS_ARTICLE_UNCOUNTABLE"

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.size < 2) return errors

        for (i in 0 until words.size - 1) {
            val curr = words[i]
            val next = words[i + 1]

            // Check superfluous article before uncountable noun (e.g. "an advice", "a furniture")
            if (curr.cleanText in setOf("a", "an") && morphology.isUncountable(next.cleanText)) {
                errors.add(
                    GrammarError(
                        ruleId = "SUPERFLUOUS_ARTICLE_UNCOUNTABLE",
                        startOffset = curr.startOffset,
                        endOffset = curr.endOffset,
                        originalStartOffset = curr.originalStartOffset,
                        originalEndOffset = curr.originalEndOffset,
                        originalText = curr.text,
                        suggestedReplacement = "",
                        category = CorrectionCategory.GRAMMAR,
                        severity = CorrectionSeverity.WARNING,
                        confidence = 0.90f,
                        explanation = "\"${next.text}\" is an uncountable noun and does not take indefinite article \"${curr.text}\"."
                    )
                )
                continue
            }

            // Check "a" vs "an"
            if (curr.cleanText == "a") {
                if (hasVowelSound(next.cleanText)) {
                    val rep = if (curr.text.first().isUpperCase()) "An" else "an"
                    errors.add(
                        GrammarError(
                            ruleId = id,
                            startOffset = curr.startOffset,
                            endOffset = curr.endOffset,
                            originalStartOffset = curr.originalStartOffset,
                            originalEndOffset = curr.originalEndOffset,
                            originalText = curr.text,
                            suggestedReplacement = rep,
                            category = category,
                            severity = severity,
                            confidence = confidence,
                            explanation = "Use \"$rep\" before \"${next.text}\" (begins with a vowel sound)."
                        )
                    )
                }
            } else if (curr.cleanText == "an") {
                if (!hasVowelSound(next.cleanText)) {
                    val rep = if (curr.text.first().isUpperCase()) "A" else "a"
                    errors.add(
                        GrammarError(
                            ruleId = id,
                            startOffset = curr.startOffset,
                            endOffset = curr.endOffset,
                            originalStartOffset = curr.originalStartOffset,
                            originalEndOffset = curr.originalEndOffset,
                            originalText = curr.text,
                            suggestedReplacement = rep,
                            category = category,
                            severity = severity,
                            confidence = confidence,
                            explanation = "Use \"$rep\" before \"${next.text}\" (begins with a consonant sound)."
                        )
                    )
                }
            }
        }

        return errors
    }

    private fun hasVowelSound(word: String): Boolean {
        if (word.isEmpty()) return false
        val lower = word.lowercase()

        // 1. Silent 'h' words that start with vowel sound
        val silentHWords = setOf("hour", "hours", "hourly", "honest", "honesty", "honestly", "honor", "honors", "honorable", "heir", "heirs", "heiress", "herb", "herbs")
        if (silentHWords.any { lower == it || lower.startsWith("$it-") }) {
            return true
        }

        // 2. Words starting with vowel letters that have a consonant 'y' sound (/j/)
        // "university", "unique", "uniform", "union", "unit", "universe", "universal"
        if (lower.startsWith("uni") && lower.length > 3 && lower[3].isLetter()) return false
        // "user", "useful", "useless", "usage", "usual", "usually", "utensil", "utility"
        if (lower.startsWith("us") && lower.length > 2 && lower in setOf("user", "users", "useful", "useless", "usual", "usually", "usage")) return false
        // "european", "eulogy", "euphemism"
        if (lower.startsWith("eu")) return false
        // "one", "once" (starts with /w/ sound)
        if (lower.startsWith("one") || lower.startsWith("once")) return false

        // 3. Regular vowel letter check
        val firstChar = lower[0]
        return firstChar in setOf('a', 'e', 'i', 'o', 'u')
    }
}
