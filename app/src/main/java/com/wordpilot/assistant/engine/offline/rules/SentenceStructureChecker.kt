package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.TokenType

class SentenceStructureChecker : Rule {

    override val id = "SENTENCE_STRUCTURE"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.WARNING
    override val confidence = 0.88f
    override val description = "Identifies sentence fragments and comma splices."

    override val positiveExamples = listOf(
        "Because he was late.",
        "Although she practiced every day.",
        "The sun is shining, the birds are singing."
    )

    override val negativeExamples = listOf(
        "Because he was late, he missed the train.",
        "Although she practiced every day, she was nervous.",
        "The sun is shining, and the birds are singing."
    )

    override fun isResponsibleFor(errorRuleId: String): Boolean =
        super.isResponsibleFor(errorRuleId) || errorRuleId in setOf("SENTENCE_FRAGMENT", "COMMA_SPLICE")

    private val subordinators = setOf(
        "because", "although", "even though", "since", "unless", "whereas"
    )

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.size < 3) return errors

        // 1. Sentence Fragment check: Starts with subordinating conjunction and has no comma separating independent clause
        val firstWord = words.first()
        if (firstWord.cleanText in subordinators) {
            val hasComma = sentence.tokens.any { it.type == TokenType.PUNCTUATION && it.text == "," }
            if (!hasComma) {
                errors.add(
                    GrammarError(
                        ruleId = "SENTENCE_FRAGMENT",
                        startOffset = firstWord.startOffset,
                        endOffset = sentence.span.end,
                        originalStartOffset = firstWord.originalStartOffset,
                        originalEndOffset = sentence.span.originalEnd,
                        originalText = sentence.span.text,
                        suggestedReplacement = null, // Don't guess full clause replacement
                        category = CorrectionCategory.GRAMMAR,
                        severity = CorrectionSeverity.WARNING,
                        confidence = 0.85f,
                        explanation = "Possible sentence fragment. A clause starting with \"${firstWord.text}\" needs an attached independent main clause."
                    )
                )
            }
        }

        // 2. Comma splice check: [Subject + Verb], [Subject + Verb] without conjunction
        // Only valid if the sentence did NOT start with a subordinating clause
        if (firstWord.cleanText !in subordinators) {
            val commaTokens = sentence.tokens.filter { it.type == TokenType.PUNCTUATION && it.text == "," }
            for (comma in commaTokens) {
                val commaIdxInWords = words.indexOfFirst { it.startOffset > comma.endOffset }
                if (commaIdxInWords > 1 && commaIdxInWords < words.size - 1) {
                    val afterCommaWord = words[commaIdxInWords]

                    // If after comma is NOT coordinating conjunction (and, but, or, so, yet, for, nor)
                    if (afterCommaWord.cleanText !in setOf("and", "but", "or", "so", "yet", "nor", "for")) {
                        var hasPredicateVerb = false

                        // Case A: Pronoun subject: ", it was" / ", they are"
                        if (afterCommaWord.cleanText in setOf("he", "she", "it", "they", "we", "i", "you")) {
                            val v = if (commaIdxInWords + 1 < words.size) words[commaIdxInWords + 1] else null
                            if (v != null && (v.tag in setOf(POSTag.VBZ, POSTag.VBD, POSTag.VBP, POSTag.MD) || v.cleanText in setOf("is", "are", "was", "were", "has", "have", "had", "can", "will"))) {
                                hasPredicateVerb = true
                            }
                        }
                        // Case B: Article/Determiner subject: ", the birds are"
                        else if (afterCommaWord.cleanText in setOf("the", "this", "that", "these", "those", "a", "an")) {
                            val v = if (commaIdxInWords + 2 < words.size) words[commaIdxInWords + 2] else null
                            if (v != null && (v.tag in setOf(POSTag.VBZ, POSTag.VBD, POSTag.VBP, POSTag.MD) || v.cleanText in setOf("is", "are", "was", "were", "has", "have", "had", "can", "will"))) {
                                hasPredicateVerb = true
                            }
                        }

                        if (hasPredicateVerb) {
                            errors.add(
                                GrammarError(
                                    ruleId = "COMMA_SPLICE",
                                    startOffset = comma.startOffset,
                                    endOffset = comma.endOffset,
                                    originalStartOffset = comma.originalStartOffset,
                                    originalEndOffset = comma.originalEndOffset,
                                    originalText = comma.text,
                                    suggestedReplacement = ";",
                                    category = CorrectionCategory.PUNCTUATION,
                                    severity = CorrectionSeverity.SUGGESTION,
                                    confidence = 0.85f,
                                    explanation = "Possible comma splice. Consider joining independent clauses with a semicolon or coordinating conjunction."
                                )
                            )
                        }
                    }
                }
            }
        }

        return errors
    }
}
