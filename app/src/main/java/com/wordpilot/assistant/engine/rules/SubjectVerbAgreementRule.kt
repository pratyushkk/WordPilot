package com.wordpilot.assistant.engine.rules

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.SentenceSpan

class SubjectVerbAgreementRule : GrammarRule {
    override val id = "SUBJECT_VERB_AGREEMENT"
    override val category = CorrectionCategory.GRAMMAR
    override val description = "Detects common subject-verb agreement errors"

    // Common patterns: singular subject + plural verb form
    private data class SVPattern(
        val pattern: Regex,
        val replacement: String,
        val explanation: String,
        val confidence: Float = 0.9f
    )

    private val patterns = listOf(
        // "She/He/It go" → "goes"
        SVPattern(
            Regex("""\b(she|he|it)\s+go\b""", RegexOption.IGNORE_CASE),
            "goes",
            "Use \"goes\" with third-person singular subject"
        ),
        // "She/He/It have" → "has" (but not "She might have")
        SVPattern(
            Regex("""\b(she|he|it)\s+have\b(?!\s+(?:to|been|had))""", RegexOption.IGNORE_CASE),
            "has",
            "Use \"has\" with third-person singular subject"
        ),
        // "She/He/It do" → "does" (but not in questions or negations easily)
        SVPattern(
            Regex("""\b(she|he|it)\s+do\s+not\b""", RegexOption.IGNORE_CASE),
            "does not",
            "Use \"does\" with third-person singular subject"
        ),
        SVPattern(
            Regex("""\b(she|he|it)\s+don't\b""", RegexOption.IGNORE_CASE),
            "doesn't",
            "Use \"doesn't\" with third-person singular subject"
        ),
        // "I has" → "I have"
        SVPattern(
            Regex("""\bI\s+has\b"""),
            "have",
            "Use \"have\" with \"I\""
        ),
        // "I goes" → "I go"
        SVPattern(
            Regex("""\bI\s+goes\b"""),
            "go",
            "Use \"go\" with \"I\""
        ),
        // "They/We has" → "have"
        SVPattern(
            Regex("""\b(they|we|you)\s+has\b""", RegexOption.IGNORE_CASE),
            "have",
            "Use \"have\" with plural subject"
        ),
        // "They/We goes" → "go"
        SVPattern(
            Regex("""\b(they|we|you)\s+goes\b""", RegexOption.IGNORE_CASE),
            "go",
            "Use \"go\" with plural subject"
        ),
        // "They/We does" → "do"
        SVPattern(
            Regex("""\b(they|we|you)\s+does\b""", RegexOption.IGNORE_CASE),
            "do",
            "Use \"do\" with plural subject"
        ),
    )

    override fun check(text: String, sentences: List<SentenceSpan>): List<Correction> {
        val corrections = mutableListOf<Correction>()

        for (svp in patterns) {
            for (match in svp.pattern.findAll(text)) {
                val subject = match.groupValues.getOrElse(1) { "" }
                val fullMatch = match.value
                val verbStart = fullMatch.indexOf(' ') + 1
                val verb = fullMatch.substring(verbStart)
                
                corrections.add(
                    Correction(
                        startOffset = match.range.first + verbStart,
                        endOffset = match.range.last + 1,
                        originalText = verb,
                        suggestedReplacement = svp.replacement,
                        category = category,
                        explanation = svp.explanation,
                        severity = CorrectionSeverity.ERROR,
                        confidence = svp.confidence,
                        ruleId = id
                    )
                )
            }
        }

        return corrections
    }
}
