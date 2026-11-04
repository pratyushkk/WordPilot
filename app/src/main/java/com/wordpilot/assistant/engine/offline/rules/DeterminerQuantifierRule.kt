package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.morphology.MorphologyAnalyzer
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class DeterminerQuantifierRule(
    private val morphology: MorphologyAnalyzer
) : Rule {

    override val id = "DETERMINER_QUANTIFIER_AGREEMENT"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.ERROR
    override val confidence = 0.95f
    override val description = "Ensure determiners and quantifiers agree with countable or uncountable nouns."

    override val positiveExamples = listOf(
        "I bought these book yesterday.",
        "She has much friends in town.",
        "He gave me many advice.",
        "There are less people here today.",
        "Every students passed the exam."
    )

    override val negativeExamples = listOf(
        "I bought these books yesterday.",
        "She has many friends in town.",
        "He gave me much advice.",
        "There are fewer people here today.",
        "Every student passed the exam."
    )

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.size < 2) return errors

        for (i in 0 until words.size - 1) {
            val curr = words[i]
            val next = words[i + 1]

            // 1. "these" / "those" + singular noun -> plural noun
            if (curr.cleanText in setOf("these", "those")) {
                if (next.tag == POSTag.NN && !morphology.isPluralNoun(next.cleanText) && !morphology.isUncountable(next.cleanText)) {
                    val plural = morphology.getPluralNoun(next.cleanText)
                    errors.add(createError(next, plural, "Plural demonstrative \"${curr.text}\" requires plural noun \"$plural\"."))
                    continue
                }
            }

            // 2. "this" / "that" + plural noun -> singular noun
            if (curr.cleanText in setOf("this", "that")) {
                if (next.tag == POSTag.NNS || morphology.isPluralNoun(next.cleanText)) {
                    val singular = morphology.getSingularNoun(next.cleanText)
                    errors.add(createError(next, singular, "Singular demonstrative \"${curr.text}\" requires singular noun \"$singular\"."))
                    continue
                }
            }

            // 3. "every" / "each" + plural noun -> singular noun
            if (curr.cleanText in setOf("every", "each")) {
                if (next.tag == POSTag.NNS || morphology.isPluralNoun(next.cleanText)) {
                    val singular = morphology.getSingularNoun(next.cleanText)
                    errors.add(createError(next, singular, "Quantifier \"${curr.text}\" is followed by a singular countable noun."))
                    continue
                }
            }

            // 4. "much" + countable plural noun -> "many"
            if (curr.cleanText == "much") {
                if (next.tag == POSTag.NNS || morphology.isPluralNoun(next.cleanText)) {
                    errors.add(createError(curr, "many", "Use \"many\" instead of \"much\" with countable plural noun \"${next.text}\"."))
                    continue
                }
            }

            // 5. "many" + uncountable noun -> "much"
            if (curr.cleanText == "many") {
                if (morphology.isUncountable(next.cleanText)) {
                    errors.add(createError(curr, "much", "Use \"much\" instead of \"many\" with uncountable noun \"${next.text}\"."))
                    continue
                }
            }

            // 6. "less" + countable plural noun -> "fewer"
            if (curr.cleanText == "less") {
                if (next.tag == POSTag.NNS || morphology.isPluralNoun(next.cleanText) || next.cleanText in setOf("people", "items", "cars", "books", "days", "hours", "problems")) {
                    errors.add(createError(curr, "fewer", "Use \"fewer\" instead of \"less\" with countable plural noun \"${next.text}\"."))
                    continue
                }
            }

            // 7. "amount of" + countable plural noun -> "number of"
            if (curr.cleanText == "amount" && next.cleanText == "of" && i + 2 < words.size) {
                val afterOf = words[i + 2]
                if (afterOf.tag == POSTag.NNS || morphology.isPluralNoun(afterOf.cleanText) || afterOf.cleanText in setOf("people", "students", "items", "cars")) {
                    errors.add(createError(curr, "number", "Use \"number of\" instead of \"amount of\" for countable items."))
                    continue
                }
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
