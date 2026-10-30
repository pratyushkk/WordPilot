package com.wordpilot.assistant.engine.offline.applier

import com.wordpilot.assistant.domain.models.Correction

class CorrectionApplier {

    fun applySingleCorrection(text: String, correction: Correction): String? {
        if (!isApplicable(text, correction)) return null
        val replacement = correction.suggestedReplacement ?: return null

        val prefix = text.substring(0, correction.startOffset)
        val suffix = text.substring(correction.endOffset)
        return prefix + replacement + suffix
    }

    fun applyMultipleCorrections(text: String, corrections: List<Correction>): String {
        // Sort descending by start offset to prevent offset drift
        val validSorted = corrections
            .filter { it.suggestedReplacement != null && it.isSafeToApply }
            .sortedByDescending { it.startOffset }

        var result = text
        for (c in validSorted) {
            val applied = applySingleCorrection(result, c)
            if (applied != null) {
                result = applied
            }
        }
        return result
    }

    fun isApplicable(text: String, correction: Correction): Boolean {
        if (correction.startOffset < 0 || correction.endOffset > text.length) return false
        if (correction.startOffset >= correction.endOffset) return false

        val currentSpan = text.substring(correction.startOffset, correction.endOffset)
        return currentSpan.equals(correction.originalText, ignoreCase = false) ||
               currentSpan.equals(correction.originalText, ignoreCase = true)
    }

    fun recalculateOffsets(corrections: List<Correction>, applied: Correction): List<Correction> {
        val replacement = applied.suggestedReplacement ?: return corrections
        val delta = replacement.length - applied.originalText.length

        return corrections.mapNotNull { c ->
            when {
                c.id == applied.id -> null
                c.endOffset <= applied.startOffset -> c // Before applied: untouched
                c.startOffset >= applied.endOffset -> c.copy( // After applied: shifted
                    startOffset = c.startOffset + delta,
                    endOffset = c.endOffset + delta
                )
                else -> null // Overlapping: invalidated
            }
        }
    }
}
