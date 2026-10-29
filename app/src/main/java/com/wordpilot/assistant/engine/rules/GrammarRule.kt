package com.wordpilot.assistant.engine.rules

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.engine.SentenceSpan

interface GrammarRule {
    val id: String
    val category: CorrectionCategory
    val description: String
    fun check(text: String, sentences: List<SentenceSpan>): List<Correction>
}
