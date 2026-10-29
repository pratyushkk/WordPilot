package com.wordpilot.assistant.engine.rules

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.SentenceSpan

class PunctuationSpacingRule : GrammarRule {
    override val id = "PUNCTUATION_SPACING"
    override val category = CorrectionCategory.PUNCTUATION
    override val description = "Detects spacing issues around punctuation"

    // Missing space after punctuation (but not for decimals, URLs, etc.)
    private val missingSpaceAfter = Regex("""([.!?,;:])([A-Za-z])""")
    // Unnecessary space before punctuation
    private val extraSpaceBefore = Regex("""\s+([.!?,;:])""")
    // Repeated punctuation
    private val repeatedPunctuation = Regex("""([.!?])\1{1,}""")

    override fun check(text: String, sentences: List<SentenceSpan>): List<Correction> {
        val corrections = mutableListOf<Correction>()

        // Missing space after punctuation
        for (match in missingSpaceAfter.findAll(text)) {
            val punct = match.groupValues[1]
            val letter = match.groupValues[2]
            
            // Skip decimals (e.g., "3.14")
            if (punct == "." && match.range.first > 0 && text[match.range.first - 1].isDigit()) continue
            // Skip URLs
            if (punct == "." && (letter.equals("c", true) || letter.equals("o", true) || letter.equals("n", true))) {
                val context = text.substring(maxOf(0, match.range.first - 5), minOf(text.length, match.range.last + 5))
                if (context.contains("www") || context.contains("http") || context.contains(".com") || context.contains(".org")) continue
            }
            // Skip abbreviations like "e.g." or "i.e."
            if (punct == ".") {
                val beforeIdx = match.range.first
                if (beforeIdx > 0 && beforeIdx < text.length - 1) {
                    val charBefore = if (beforeIdx > 0) text[beforeIdx - 1] else ' '
                    if (charBefore.isLetter() && text[match.range.last].isLetter() &&
                        (beforeIdx < 2 || !text[beforeIdx - 2].isLetter())) continue
                }
            }

            corrections.add(
                Correction(
                    startOffset = match.range.first,
                    endOffset = match.range.last + 1,
                    originalText = match.value,
                    suggestedReplacement = "$punct $letter",
                    category = category,
                    explanation = "Add a space after \"$punct\"",
                    severity = CorrectionSeverity.WARNING,
                    confidence = 0.85f,
                    ruleId = "$id.MISSING_SPACE_AFTER"
                )
            )
        }

        // Unnecessary space before punctuation
        for (match in extraSpaceBefore.findAll(text)) {
            val punct = match.groupValues[1]
            // Don't flag space before opening parens etc. — only closing/terminal punct
            if (punct !in setOf(".", ",", "!", "?", ";", ":")) continue
            
            corrections.add(
                Correction(
                    startOffset = match.range.first,
                    endOffset = match.range.last + 1,
                    originalText = match.value,
                    suggestedReplacement = punct,
                    category = category,
                    explanation = "Remove space before \"$punct\"",
                    severity = CorrectionSeverity.WARNING,
                    confidence = 0.9f,
                    ruleId = "$id.EXTRA_SPACE_BEFORE"
                )
            )
        }

        // Repeated punctuation
        for (match in repeatedPunctuation.findAll(text)) {
            // Allow "..." (ellipsis) and "!!" / "??" in informal text
            if (match.value == "..." || match.value == "..") continue
            
            corrections.add(
                Correction(
                    startOffset = match.range.first,
                    endOffset = match.range.last + 1,
                    originalText = match.value,
                    suggestedReplacement = match.groupValues[1],
                    category = category,
                    explanation = "Remove repeated punctuation",
                    severity = CorrectionSeverity.SUGGESTION,
                    confidence = 0.8f,
                    ruleId = "$id.REPEATED"
                )
            )
        }

        return corrections
    }
}
