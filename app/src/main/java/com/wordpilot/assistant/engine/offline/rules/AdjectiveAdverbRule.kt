package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class AdjectiveAdverbRule : Rule {

    override val id = "ADJECTIVE_ADVERB_MISUSE"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.ERROR
    override val confidence = 0.94f
    override val description = "Avoid double comparatives and ensure adverbs modify action verbs."

    override val positiveExamples = listOf(
        "This solution is more better than the previous one.",
        "It was the most highest mountain in the region.",
        "He speaks English good.",
        "She drove real slow on the icy road."
    )

    override val negativeExamples = listOf(
        "This solution is better than the previous one.",
        "It was the highest mountain in the region.",
        "He speaks English well.",
        "She drove really slowly on the icy road."
    )

    override fun isResponsibleFor(errorRuleId: String): Boolean =
        super.isResponsibleFor(errorRuleId) || errorRuleId in setOf("DOUBLE_COMPARATIVE", "DOUBLE_SUPERLATIVE")

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.size < 2) return errors

        for (i in 0 until words.size - 1) {
            val curr = words[i]
            val next = words[i + 1]

            // 1. Double comparatives: "more" + -er comparative adjective
            if (curr.cleanText == "more" && (next.tag == POSTag.JJR || next.cleanText.endsWith("er") && next.cleanText in setOf("better", "easier", "faster", "slower", "harder", "simpler", "cheaper", "clearer", "taller", "shorter", "stronger"))) {
                errors.add(
                    GrammarError(
                        ruleId = "DOUBLE_COMPARATIVE",
                        startOffset = curr.startOffset,
                        endOffset = curr.endOffset,
                        originalStartOffset = curr.originalStartOffset,
                        originalEndOffset = curr.originalEndOffset,
                        originalText = curr.text,
                        suggestedReplacement = "",
                        category = category,
                        severity = severity,
                        confidence = 0.98f,
                        explanation = "Remove \"more\"; \"${next.text}\" is already in comparative form."
                    )
                )
                continue
            }

            // 2. Double superlatives: "most" + -est superlative adjective
            if (curr.cleanText == "most" && (next.tag == POSTag.JJS || next.cleanText.endsWith("est") && next.cleanText in setOf("highest", "best", "fastest", "slowest", "easiest", "hardest", "cheapest", "simplest", "clearest", "strongest"))) {
                errors.add(
                    GrammarError(
                        ruleId = "DOUBLE_SUPERLATIVE",
                        startOffset = curr.startOffset,
                        endOffset = curr.endOffset,
                        originalStartOffset = curr.originalStartOffset,
                        originalEndOffset = curr.originalEndOffset,
                        originalText = curr.text,
                        suggestedReplacement = "",
                        category = category,
                        severity = severity,
                        confidence = 0.98f,
                        explanation = "Remove \"most\"; \"${next.text}\" is already in superlative form."
                    )
                )
                continue
            }

            // 3. Misused "good" for adverb "well" after action verbs
            if (curr.cleanText in setOf("speaks", "speak", "sings", "sing", "works", "work", "played", "play", "did", "does", "done", "written", "write") && next.cleanText == "good") {
                errors.add(createError(next, "well", "Use adverb \"well\" instead of adjective \"good\" to modify action verb \"${curr.text}\"."))
                continue
            }
            if (curr.cleanText == "english" && i > 0 && words[i - 1].cleanText in setOf("speaks", "speak", "writes", "write") && next.cleanText == "good") {
                errors.add(createError(next, "well", "Use adverb \"well\" instead of adjective \"good\" to modify \"${words[i - 1].text}\"."))
                continue
            }

            // 4. "real" instead of "really" modifying adjective/adverb
            if (curr.cleanText == "real" && (next.tag == POSTag.JJ || next.tag == POSTag.RB || next.cleanText in setOf("good", "bad", "slow", "fast", "quick", "hard", "nice", "big", "small"))) {
                errors.add(createError(curr, "really", "Use adverb \"really\" instead of adjective \"real\" before an adjective or adverb."))
                continue
            }
        }

        return errors
    }

    private fun createError(token: Token, replacement: String, exp: String): GrammarError {
        val formatted = TextNormalizer.applyCasing(replacement, token.casing)
        return GrammarError(
            ruleId = id,
            startOffset = token.startOffset,
            endOffset = token.endOffset,
            originalStartOffset = token.originalStartOffset,
            originalEndOffset = token.originalEndOffset,
            originalText = token.text,
            suggestedReplacement = formatted,
            category = category,
            severity = severity,
            confidence = confidence,
            explanation = exp
        )
    }
}
