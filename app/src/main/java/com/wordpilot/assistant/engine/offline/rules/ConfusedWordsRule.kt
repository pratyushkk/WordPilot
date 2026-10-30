package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class ConfusedWordsRule : Rule {

    override val id = "CONFUSED_WORDS_AND_COLLOCATIONS"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.ERROR
    override val confidence = 0.98f
    override val description = "Detects commonly confused homophones, miscollocations, and modal-of errors."

    override val positiveExamples = listOf(
        "You should of told me earlier.",
        "This is better then that one.",
        "Don't loose your keys.",
        "It had a significant affect on us.",
        "It depends of your schedule.",
        "She is interested for science."
    )

    override val negativeExamples = listOf(
        "You should have told me earlier.",
        "This is better than that one.",
        "Don't lose your keys.",
        "It had a significant effect on us.",
        "It depends on your schedule.",
        "She is interested in science."
    )

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.size < 2) return errors

        for (i in 0 until words.size - 1) {
            val curr = words[i]
            val next = words[i + 1]

            // 1. "could of", "should of", "would of", "might of", "must of" -> "have"
            if (curr.cleanText in setOf("could", "should", "would", "might", "must") && next.cleanText == "of") {
                errors.add(createError(next, "have", "Write \"have\" instead of \"of\" after modal \"${curr.text}\"."))
                continue
            }

            // 2. "than" vs "then"
            // After comparative adjective/adverb e.g. "better then", "more then", "faster then" -> "than"
            if (curr.tag in setOf(POSTag.JJR, POSTag.RBR) || curr.cleanText in setOf("better", "worse", "more", "less", "faster", "slower", "earlier", "later", "bigger", "smaller", "taller", "rather", "other")) {
                if (next.cleanText == "then") {
                    errors.add(createError(next, "than", "Use \"than\" for comparisons rather than \"then\"."))
                    continue
                }
            } else if (curr.cleanText == "and" && next.cleanText == "than") {
                errors.add(createError(next, "then", "Use \"then\" to indicate subsequent time or sequence."))
                continue
            }

            // 3. "loose" vs "lose"
            if (curr.cleanText in setOf("to", "will", "would", "did", "can", "don't", "dont", "never") && next.cleanText == "loose") {
                errors.add(createError(next, "lose", "Did you mean verb \"lose\" rather than adjective \"loose\"?"))
                continue
            }

            // 4. "affect" vs "effect"
            if (curr.cleanText in setOf("an", "a", "the", "significant", "major", "big", "profound", "positive", "negative") && next.cleanText == "affect") {
                errors.add(createError(next, "effect", "Use noun \"effect\" instead of verb \"affect\"."))
                continue
            } else if (curr.cleanText in setOf("will", "would", "to", "can", "could", "may", "might") && next.cleanText == "effect") {
                errors.add(createError(next, "affect", "Use verb \"affect\" instead of noun \"effect\"."))
                continue
            }

            // 5. "accept" vs "except"
            if (curr.cleanText in setOf("everyone", "everybody", "all", "nothing", "nobody") && next.cleanText == "accept") {
                errors.add(createError(next, "except", "Use preposition \"except\" (meaning excluding) rather than \"accept\"."))
                continue
            }

            // 6. "principal" vs "principle"
            if (curr.cleanText in setOf("school", "high", "elementary") && next.cleanText == "principle") {
                errors.add(createError(next, "principal", "Use \"principal\" for the head of a school."))
                continue
            }

            // 7. "advice" (noun) vs "advise" (verb)
            if (curr.cleanText in setOf("i", "we", "they", "to") && next.cleanText == "advice") {
                errors.add(createError(next, "advise", "Use verb \"advise\" rather than noun \"advice\"."))
                continue
            } else if (curr.cleanText in setOf("some", "good", "my", "your", "give", "take") && next.cleanText == "advise") {
                errors.add(createError(next, "advice", "Use noun \"advice\" rather than verb \"advise\"."))
                continue
            }

            // 8. Preposition Collocations
            // "depend of" / "depends of" -> "on"
            if (curr.cleanText in setOf("depend", "depends", "depended", "depending") && next.cleanText in setOf("of", "from")) {
                errors.add(createError(next, "on", "The verb \"${curr.text}\" collocates with preposition \"on\"."))
                continue
            }
            // "interested for" -> "in"
            if (curr.cleanText == "interested" && next.cleanText in setOf("for", "about", "to", "at")) {
                if (next.cleanText != "to" || (i + 2 < words.size && words[i + 2].cleanText in setOf("the", "a", "this", "my"))) {
                    errors.add(createError(next, "in", "The adjective \"interested\" collocates with preposition \"in\"."))
                    continue
                }
            }
            // "congratulate for" -> "on"
            if (curr.cleanText in setOf("congratulate", "congratulated", "congratulations") && next.cleanText in setOf("for", "to")) {
                errors.add(createError(next, "on", "Use preposition \"on\" with \"${curr.text}\"."))
                continue
            }
            // "listen at" -> "listen to"
            if (curr.cleanText in setOf("listen", "listens", "listened", "listening") && next.cleanText == "at") {
                errors.add(createError(next, "to", "Use preposition \"to\" with \"${curr.text}\"."))
                continue
            }
            // "responsible of" -> "responsible for"
            if (curr.cleanText == "responsible" && next.cleanText == "of") {
                errors.add(createError(next, "for", "\"Responsible\" collocates with preposition \"for\"."))
                continue
            }
            // "different than / to" -> "different from"
            if (curr.cleanText == "different" && next.cleanText in setOf("than", "to")) {
                errors.add(createError(next, "from", "\"Different\" standardly takes preposition \"from\"."))
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
