package com.wordpilot.assistant.engine.rules

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.SentenceSpan

class CapitalizationRule : GrammarRule {
    override val id = "CAPITALIZATION"
    override val category = CorrectionCategory.CAPITALIZATION
    override val description = "Detects missing capitalization at sentence start"

    private val alwaysCapitalized = setOf("i")

    override fun check(text: String, sentences: List<SentenceSpan>): List<Correction> {
        val corrections = mutableListOf<Correction>()

        for (sentence in sentences) {
            val trimmedStart = sentence.text.indexOfFirst { it.isLetter() }
            if (trimmedStart < 0) continue
            
            val firstChar = sentence.text[trimmedStart]
            if (firstChar.isLowerCase()) {
                val absOffset = sentence.start + trimmedStart
                corrections.add(
                    Correction(
                        startOffset = absOffset,
                        endOffset = absOffset + 1,
                        originalText = firstChar.toString(),
                        suggestedReplacement = firstChar.uppercase(),
                        category = category,
                        explanation = "Capitalize the first letter of the sentence",
                        severity = CorrectionSeverity.WARNING,
                        confidence = 0.85f,
                        ruleId = id
                    )
                )
            }
        }

        // Check standalone "i" → "I"
        val iPattern = Regex("""\bi\b""")
        for (match in iPattern.findAll(text)) {
            // Make sure it's not inside a word (the \b should handle this)
            val before = if (match.range.first > 0) text[match.range.first - 1] else ' '
            val after = if (match.range.last + 1 < text.length) text[match.range.last + 1] else ' '
            if (!before.isLetter() && !after.isLetter()) {
                corrections.add(
                    Correction(
                        startOffset = match.range.first,
                        endOffset = match.range.last + 1,
                        originalText = "i",
                        suggestedReplacement = "I",
                        category = category,
                        explanation = "\"I\" should always be capitalized",
                        severity = CorrectionSeverity.ERROR,
                        confidence = 0.98f,
                        ruleId = "$id.PRONOUN_I"
                    )
                )
            }
        }

        return corrections
    }
}
