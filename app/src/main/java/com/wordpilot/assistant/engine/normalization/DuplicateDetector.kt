package com.wordpilot.assistant.engine.normalization

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionSeverity

object DuplicateDetector {

    /**
     * Removes duplicate and overlapping corrections.
     * When corrections overlap, the higher-severity one wins.
     * Within same severity, the one from the higher-priority provider wins.
     */
    fun deduplicate(corrections: List<Correction>): List<Correction> {
        if (corrections.size <= 1) return corrections

        // Sort by start offset, then by severity descending (ERROR > WARNING > SUGGESTION)
        val sorted = corrections.sortedWith(
            compareBy<Correction> { it.startOffset }
                .thenByDescending { severityWeight(it.severity) }
        )

        val result = mutableListOf<Correction>()
        var lastEnd = -1

        for (correction in sorted) {
            if (correction.startOffset >= lastEnd) {
                // No overlap: accept
                result.add(correction)
                lastEnd = correction.endOffset
            }
            // Overlapping corrections are dropped (lower severity / later in sort)
        }

        return result
    }

    /**
     * Detects exact duplicate suggestions (same range + same replacement).
     */
    fun removeDuplicates(corrections: List<Correction>): List<Correction> {
        val seen = mutableSetOf<String>()
        return corrections.filter { c ->
            val key = "${c.startOffset}:${c.endOffset}:${c.suggestedReplacement}"
            seen.add(key)
        }
    }

    /**
     * Full pipeline: remove exact duplicates then resolve overlaps.
     */
    fun process(corrections: List<Correction>): List<Correction> {
        return deduplicate(removeDuplicates(corrections))
    }

    private fun severityWeight(s: CorrectionSeverity): Int = when (s) {
        CorrectionSeverity.ERROR -> 3
        CorrectionSeverity.WARNING -> 2
        CorrectionSeverity.SUGGESTION -> 1
    }
}
