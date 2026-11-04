package com.wordpilot.assistant.engine.rules

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.SentenceSpan

class RepeatedWordRule : GrammarRule {
    override val id = "REPEATED_WORD"
    override val category = CorrectionCategory.GRAMMAR
    override val description = "Detects repeated consecutive words"

    private val wordPattern = Regex("""\b(\w+)\s+\1\b""", RegexOption.IGNORE_CASE)

    override fun check(text: String, sentences: List<SentenceSpan>): List<Correction> {
        val corrections = mutableListOf<Correction>()
        
        for (match in wordPattern.findAll(text)) {
            val repeatedWord = match.groupValues[1]
            // Skip intentional repetitions like "that that" in some contexts
            if (repeatedWord.length <= 1) continue
            
            corrections.add(
                Correction(
                    startOffset = match.range.first,
                    endOffset = match.range.last + 1,
                    originalText = match.value,
                    suggestedReplacement = repeatedWord,
                    category = category,
                    explanation = "Remove repeated word \"$repeatedWord\"",
                    severity = CorrectionSeverity.ERROR,
                    confidence = 0.95f,
                    ruleId = id
                )
            )
        }
        
        return corrections
    }
}
