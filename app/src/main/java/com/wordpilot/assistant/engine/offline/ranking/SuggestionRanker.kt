package com.wordpilot.assistant.engine.offline.ranking

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.dictionary.EnglishDictionary
import com.wordpilot.assistant.engine.offline.model.GrammarError

class SuggestionRanker(
    private val dictionary: EnglishDictionary
) {

    fun rankAndFilter(
        errors: List<GrammarError>,
        activeCursorOffset: Int? = null,
        minConfidence: Float = 0.70f
    ): List<Correction> {
        if (errors.isEmpty()) return emptyList()

        // 1. Filter out suppressed items
        val filtered = errors.filter { err ->
            // Confidence threshold
            if (err.confidence < minConfidence) return@filter false

            // User personal dictionary suppression
            if (dictionary.hasCustomWord(err.originalText)) return@filter false

            // Incomplete word at active cursor suppression
            if (activeCursorOffset != null) {
                // If cursor is inside or immediately at the end of this token, user might be actively typing it
                if (activeCursorOffset in err.originalStartOffset..err.originalEndOffset) {
                    if (err.category == CorrectionCategory.SPELLING) {
                        return@filter false
                    }
                }
            }

            true
        }

        if (filtered.isEmpty()) return emptyList()

        // 2. Resolve overlapping spans (higher severity > higher confidence > longer span)
        val sorted = filtered.sortedWith(
            compareBy<GrammarError> { it.originalStartOffset }
                .thenByDescending { severityWeight(it.severity) }
                .thenByDescending { it.confidence }
                .thenByDescending { it.originalEndOffset - it.originalStartOffset }
        )

        val nonOverlapping = mutableListOf<GrammarError>()
        var lastEnd = -1

        for (err in sorted) {
            if (err.originalStartOffset >= lastEnd) {
                nonOverlapping.add(err)
                lastEnd = err.originalEndOffset
            }
        }

        // 3. Convert to domain Corrections
        return nonOverlapping.map { it.toCorrection() }
    }

    private fun severityWeight(severity: CorrectionSeverity): Int = when (severity) {
        CorrectionSeverity.ERROR -> 3
        CorrectionSeverity.WARNING -> 2
        CorrectionSeverity.SUGGESTION -> 1
    }
}
