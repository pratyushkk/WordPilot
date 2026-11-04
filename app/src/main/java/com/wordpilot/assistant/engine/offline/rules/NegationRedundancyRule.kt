package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class NegationRedundancyRule : Rule {

    override val id = "NEGATION_AND_REDUNDANCY"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.ERROR
    override val confidence = 0.96f
    override val description = "Avoid double negatives and repeated duplicate words."

    override val positiveExamples = listOf(
        "I don't know nothing about the incident.",
        "We didn't see nobody at the park.",
        "She can't find none of her notes.",
        "They arrived in the the morning."
    )

    override val negativeExamples = listOf(
        "I don't know anything about the incident.",
        "We didn't see anybody at the park.",
        "She can't find any of her notes.",
        "They arrived in the morning.",
        "He knew that that was the right answer.",
        "She had had a long day."
    )

    override fun isResponsibleFor(errorRuleId: String): Boolean =
        super.isResponsibleFor(errorRuleId) || errorRuleId in setOf("DOUBLE_NEGATIVE", "REPEATED_WORD")

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.size < 2) return errors

        for (i in 0 until words.size - 1) {
            val curr = words[i]
            val next = words[i + 1]

            // 1. Repeated duplicate words
            if (curr.cleanText == next.cleanText) {
                // Legitimate exceptions: "that that", "had had"
                if (curr.cleanText !in setOf("that", "had")) {
                    errors.add(
                        GrammarError(
                            ruleId = "REPEATED_WORD",
                            startOffset = curr.startOffset,
                            endOffset = next.endOffset,
                            originalStartOffset = curr.originalStartOffset,
                            originalEndOffset = next.originalEndOffset,
                            originalText = "${curr.text} ${next.text}",
                            suggestedReplacement = curr.text,
                            category = CorrectionCategory.STYLE,
                            severity = CorrectionSeverity.WARNING,
                            confidence = 0.98f,
                            explanation = "Duplicate word \"${next.text}\". Remove the extra word."
                        )
                    )
                    continue
                }
            }

            // 2. Double negatives (negative auxiliary + negative pronoun/adverb)
            // e.g. "don't know nothing" -> "anything"
            val isNegativeAuxiliary = curr.cleanText.endsWith("n't") || curr.cleanText in setOf("not", "never", "hardly", "scarcely", "barely")
            if (isNegativeAuxiliary) {
                // Check immediate next or word after next
                val target = if (next.cleanText in setOf("nothing", "nobody", "no", "nowhere", "none")) {
                    next
                } else if (i + 2 < words.size && words[i + 2].cleanText in setOf("nothing", "nobody", "no", "nowhere", "none")) {
                    words[i + 2]
                } else null

                if (target != null) {
                    val positiveRepl = when (target.cleanText) {
                        "nothing" -> "anything"
                        "nobody" -> "anybody"
                        "nowhere" -> "anywhere"
                        "none" -> "any"
                        "no" -> "any"
                        else -> target.cleanText
                    }
                    val formatted = TextNormalizer.applyCasing(positiveRepl, target.casing)
                    errors.add(
                        GrammarError(
                            ruleId = "DOUBLE_NEGATIVE",
                            startOffset = target.startOffset,
                            endOffset = target.endOffset,
                            originalStartOffset = target.originalStartOffset,
                            originalEndOffset = target.originalEndOffset,
                            originalText = target.text,
                            suggestedReplacement = formatted,
                            category = category,
                            severity = severity,
                            confidence = confidence,
                            explanation = "Avoid double negatives; use \"$formatted\" after negative particle \"${curr.text}\"."
                        )
                    )
                }
            }
        }

        return errors
    }
}
