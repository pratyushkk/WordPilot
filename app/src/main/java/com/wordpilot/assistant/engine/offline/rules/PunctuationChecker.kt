package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.model.TokenType
import com.wordpilot.assistant.engine.offline.model.WordCasing

class PunctuationChecker : Rule {

    override val id = "PUNCTUATION_AND_CAPITALIZATION"
    override val category = CorrectionCategory.PUNCTUATION
    override val severity = CorrectionSeverity.WARNING
    override val confidence = 0.95f
    override val description = "Ensure correct punctuation spacing, introductory commas, and sentence capitalization."

    override val positiveExamples = listOf(
        "hello world.",
        "Yes , I agree with you.",
        "First,second and third.",
        "he and i arrived early.",
        "However we decided to postpone the meeting."
    )

    override val negativeExamples = listOf(
        "Hello world.",
        "Yes, I agree with you.",
        "First, second and third.",
        "He and I arrived early.",
        "However, we decided to postpone the meeting."
    )

    override fun isResponsibleFor(errorRuleId: String): Boolean =
        super.isResponsibleFor(errorRuleId) ||
        errorRuleId.startsWith("CAPITALIZATION") ||
        errorRuleId.startsWith("PUNCTUATION") ||
        errorRuleId in setOf("SPACE_BEFORE_PUNCTUATION", "INTRODUCTORY_COMMA", "DUPLICATE_PUNCTUATION")

    private val introductoryTransitions = setOf(
        "however", "furthermore", "moreover", "therefore", "consequently",
        "nevertheless", "meanwhile", "additionally", "firstly", "secondly", "finally"
    )

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val tokens = sentence.tokens
        if (tokens.isEmpty()) return errors

        // 1. Sentence Capitalization
        val firstWord = tokens.firstOrNull { it.isWord }
        if (firstWord != null && firstWord.text.isNotEmpty() && firstWord.text.first().isLowerCase()) {
            val capitalizedChar = firstWord.text.substring(0, 1).uppercase()
            errors.add(
                GrammarError(
                    ruleId = "CAPITALIZATION.SENTENCE_START",
                    startOffset = firstWord.startOffset,
                    endOffset = firstWord.startOffset + 1,
                    originalStartOffset = firstWord.originalStartOffset,
                    originalEndOffset = firstWord.originalStartOffset + 1,
                    originalText = firstWord.text.substring(0, 1),
                    suggestedReplacement = capitalizedChar,
                    category = CorrectionCategory.CAPITALIZATION,
                    severity = CorrectionSeverity.WARNING,
                    confidence = 0.98f,
                    explanation = "Sentences should start with an uppercase letter."
                )
            )
        }

        // 2. Standalone lowercase "i"
        for (token in tokens) {
            if (token.type == TokenType.WORD && token.text == "i") {
                errors.add(
                    GrammarError(
                        ruleId = "CAPITALIZATION.PRONOUN_I",
                        startOffset = token.startOffset,
                        endOffset = token.endOffset,
                        originalStartOffset = token.originalStartOffset,
                        originalEndOffset = token.originalEndOffset,
                        originalText = "i",
                        suggestedReplacement = "I",
                        category = CorrectionCategory.CAPITALIZATION,
                        severity = CorrectionSeverity.ERROR,
                        confidence = 0.99f,
                        explanation = "The pronoun \"I\" should always be capitalized."
                    )
                )
            }
        }

        // 3. Spacing around punctuation
        for (i in tokens.indices) {
            val t = tokens[i]

            // Check space before punctuation: "word ,"
            if (t.type == TokenType.PUNCTUATION && t.text in setOf(",", ".", "!", "?", ";", ":")) {
                if (i > 0 && tokens[i - 1].type == TokenType.WHITESPACE) {
                    val wsToken = tokens[i - 1]
                    errors.add(
                        GrammarError(
                            ruleId = "SPACE_BEFORE_PUNCTUATION",
                            startOffset = wsToken.startOffset,
                            endOffset = t.endOffset,
                            originalStartOffset = wsToken.originalStartOffset,
                            originalEndOffset = t.originalEndOffset,
                            originalText = wsToken.text + t.text,
                            suggestedReplacement = t.text,
                            category = CorrectionCategory.PUNCTUATION,
                            severity = CorrectionSeverity.WARNING,
                            confidence = 0.96f,
                            explanation = "Remove unnecessary space before punctuation mark \"${t.text}\"."
                        )
                    )
                }

                // Check missing space after comma/semicolon/colon e.g. "word,word"
                if (t.text in setOf(",", ";", ":")) {
                    if (i + 1 < tokens.size && tokens[i + 1].type == TokenType.WORD) {
                        val nextWord = tokens[i + 1]
                        val nextFirstChar = nextWord.text.substring(0, 1)
                        errors.add(
                            GrammarError(
                                ruleId = "PUNCTUATION_SPACING.MISSING_SPACE",
                                startOffset = t.startOffset,
                                endOffset = nextWord.startOffset + 1,
                                originalStartOffset = t.originalStartOffset,
                                originalEndOffset = nextWord.originalStartOffset + 1,
                                originalText = "${t.text}$nextFirstChar",
                                suggestedReplacement = "${t.text} $nextFirstChar",
                                category = CorrectionCategory.PUNCTUATION,
                                severity = CorrectionSeverity.WARNING,
                                confidence = 0.96f,
                                explanation = "Insert a space after \"${t.text}\"."
                            )
                        )
                    }
                }
            }

            // Duplicate punctuation e.g. ",," or ";;"
            if (t.type == TokenType.PUNCTUATION && (t.text == ",," || t.text == ";;" || t.text == "::")) {
                val singlePunct = t.text.substring(0, 1)
                errors.add(
                    GrammarError(
                        ruleId = "DUPLICATE_PUNCTUATION",
                        startOffset = t.startOffset,
                        endOffset = t.endOffset,
                        originalStartOffset = t.originalStartOffset,
                        originalEndOffset = t.originalEndOffset,
                        originalText = t.text,
                        suggestedReplacement = singlePunct,
                        category = CorrectionCategory.PUNCTUATION,
                        severity = CorrectionSeverity.WARNING,
                        confidence = 0.98f,
                        explanation = "Duplicate punctuation mark \"${t.text}\"."
                    )
                )
            }
        }

        // 4. Missing comma after introductory transition word (e.g. "However we decided")
        val words = sentence.wordTokens
        if (words.size >= 2) {
            val first = words[0]
            val second = words[1]
            if (first.cleanText in introductoryTransitions) {
                // Check if followed by comma
                val firstTokenIdx = tokens.indexOf(first)
                val hasComma = if (firstTokenIdx != -1 && firstTokenIdx + 1 < tokens.size) {
                    val nextTok = tokens[firstTokenIdx + 1]
                    nextTok.text == "," || (firstTokenIdx + 2 < tokens.size && tokens[firstTokenIdx + 2].text == ",")
                } else false

                if (!hasComma) {
                    errors.add(
                        GrammarError(
                            ruleId = "INTRODUCTORY_COMMA",
                            startOffset = first.startOffset,
                            endOffset = first.endOffset,
                            originalStartOffset = first.originalStartOffset,
                            originalEndOffset = first.originalEndOffset,
                            originalText = first.text,
                            suggestedReplacement = "${first.text},",
                            category = CorrectionCategory.PUNCTUATION,
                            severity = CorrectionSeverity.SUGGESTION,
                            confidence = 0.90f,
                            explanation = "Introductory transitional word \"${first.text}\" should be followed by a comma."
                        )
                    )
                }
            }
        }

        return errors
    }
}
