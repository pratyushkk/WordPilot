package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class PronounRule : Rule {

    override val id = "PRONOUN_CASE_AND_CONTRACTION"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.ERROR
    override val confidence = 0.96f
    override val description = "Ensure correct pronoun case and distinguish possessives from contractions."

    override val positiveExamples = listOf(
        "Him and I went to the store.",
        "This is between you and I.",
        "Your welcome to join us.",
        "Its raining outside today.",
        "The dog wagged it's tail.",
        "Their going to miss the train.",
        "Who's jacket is this?"
    )

    override val negativeExamples = listOf(
        "He and I went to the store.",
        "This is between you and me.",
        "You're welcome to join us.",
        "It's raining outside today.",
        "The dog wagged its tail.",
        "They're going to miss the train.",
        "Whose jacket is this?"
    )

    override fun isResponsibleFor(errorRuleId: String): Boolean =
        super.isResponsibleFor(errorRuleId) || errorRuleId.startsWith("CONFUSED_WORDS")

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.isEmpty()) return errors

        for (i in words.indices) {
            val curr = words[i]
            val prev = if (i > 0) words[i - 1] else null
            val next = if (i + 1 < words.size) words[i + 1] else null

            // 1. Compound subjects: "Him and I" -> "He and I", "Her and I" -> "She and I", "Me and John" -> "John and I"
            if (curr.cleanText in setOf("him", "her", "me", "them", "us") && next != null && next.cleanText == "and" && i + 2 < words.size) {
                // If followed by verb, this is a subject clause
                val secondSubj = words[i + 2]
                val afterSecond = if (i + 3 < words.size) words[i + 3] else null
                if (afterSecond != null && (afterSecond.tag in setOf(POSTag.VB, POSTag.VBZ, POSTag.VBP, POSTag.VBD, POSTag.MD) || afterSecond.cleanText in setOf("went", "are", "were", "is", "was", "will", "have", "had", "can"))) {
                    val subjRepl = when (curr.cleanText) {
                        "him" -> "he"
                        "her" -> "she"
                        "me" -> "I"
                        "them" -> "they"
                        "us" -> "we"
                        else -> curr.cleanText
                    }
                    errors.add(createError(curr, subjRepl, "Use subjective pronoun \"$subjRepl\" in subject position."))
                }
            }

            // 2. Preposition + subject pronoun: "between you and I" -> "between you and me"
            if (prev != null && prev.cleanText in setOf("between", "for", "with", "to", "against", "without") && curr.cleanText in setOf("he", "she", "they", "we")) {
                val objRepl = when (curr.cleanText) {
                    "he" -> "him"
                    "she" -> "her"
                    "they" -> "them"
                    "we" -> "us"
                    else -> curr.cleanText
                }
                errors.add(createError(curr, objRepl, "Use objective pronoun \"$objRepl\" after preposition \"${prev.text}\"."))
            } else if (prev != null && prev.cleanText == "and" && i >= 2 && words[i - 2].cleanText in setOf("you", "him", "her") && i >= 3 && words[i - 3].cleanText in setOf("between", "for", "with", "to")) {
                if (curr.cleanText == "i") {
                    val rep = if (prev.casing == com.wordpilot.assistant.engine.offline.model.WordCasing.UPPERCASE) "ME" else "me"
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
                            explanation = "Use objective pronoun \"me\" in prepositional phrase \"${words[i - 3].text} ${words[i - 2].text} and me\"."
                        )
                    )
                }
            }

            // 3. YOUR vs YOU'RE
            if (curr.cleanText == "your" && next != null) {
                if (next.cleanText in setOf("welcome", "right", "wrong", "invited", "coming", "going", "beautiful", "amazing", "smart", "late", "early", "done") || next.tag == POSTag.VBG) {
                    errors.add(createError(curr, "you're", "Did you mean contraction \"you're\" (you are) instead of possessive \"your\"?"))
                }
            } else if (curr.cleanText in setOf("you're", "youre") && next != null) {
                if (next.tag == POSTag.NN && next.cleanText !in setOf("welcome", "right", "wrong", "done")) {
                    errors.add(createError(curr, "your", "Did you mean possessive \"your\" instead of contraction \"you're\"?"))
                }
            }

            // 4. ITS vs IT'S
            if (curr.cleanText == "its" && next != null) {
                if (next.cleanText in setOf("a", "an", "the", "not", "raining", "cold", "hot", "good", "bad", "time", "clear", "obvious", "important", "true", "possible") || next.tag == POSTag.VBG || next.tag == POSTag.JJ) {
                    errors.add(createError(curr, "it's", "Did you mean contraction \"it's\" (it is) instead of possessive \"its\"?"))
                }
            } else if (curr.cleanText in setOf("it's", "it's") && next != null) {
                if (next.tag == POSTag.NN && next.cleanText in setOf("tail", "name", "color", "head", "body", "size", "weight", "price", "wheels", "legs", "eyes", "purpose")) {
                    errors.add(createError(curr, "its", "Did you mean possessive \"its\" instead of contraction \"it's\" (it is)?"))
                }
            }

            // 5. THEIR vs THERE vs THEY'RE
            if (curr.cleanText == "their" && next != null) {
                if (next.cleanText in setOf("going", "coming", "not", "all", "ready", "here", "there") || next.tag == POSTag.VBG) {
                    val formatted = TextNormalizer.applyCasing("they're", curr.casing)
                    errors.add(
                        GrammarError(
                            ruleId = "CONFUSED_WORDS.THEIR_THEYRE",
                            startOffset = curr.startOffset,
                            endOffset = curr.endOffset,
                            originalStartOffset = curr.originalStartOffset,
                            originalEndOffset = curr.originalEndOffset,
                            originalText = curr.text,
                            suggestedReplacement = formatted,
                            category = category,
                            severity = severity,
                            confidence = confidence,
                            explanation = "Did you mean contraction \"they're\" (they are) instead of possessive \"their\"?"
                        )
                    )
                }
            } else if (curr.cleanText == "there" && prev != null && prev.cleanText in setOf("in", "at", "from", "to", "for") && next != null && next.tag == POSTag.NN) {
                // "in there car" -> "their"
                errors.add(createError(curr, "their", "Did you mean possessive \"their\" instead of \"there\"?"))
            }

            // 6. WHOSE vs WHO'S
            if (curr.cleanText == "whose" && next != null) {
                if (next.cleanText in setOf("coming", "there", "that", "this", "going", "ready", "next") || next.tag == POSTag.VBG) {
                    errors.add(createError(curr, "who's", "Did you mean contraction \"who's\" (who is) instead of possessive \"whose\"?"))
                }
            } else if (curr.cleanText in setOf("who's", "whos") && next != null) {
                if (next.tag == POSTag.NN && next.cleanText !in setOf("coming", "going", "ready", "there", "next")) {
                    errors.add(createError(curr, "whose", "Did you mean possessive \"whose\" instead of contraction \"who's\" (who is)?"))
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
