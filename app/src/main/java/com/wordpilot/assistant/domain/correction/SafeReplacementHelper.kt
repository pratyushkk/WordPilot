package com.wordpilot.assistant.domain.correction

import com.wordpilot.assistant.domain.models.Correction

class SafeReplacementHelper {
    
    fun applyCorrection(text: String, correction: Correction): String? {
        // Validate the correction is still applicable
        if (!validateCorrection(text, correction)) return null
        
        val before = text.substring(0, correction.startOffset)
        val after = text.substring(correction.endOffset)
        val replacement = correction.suggestedReplacement ?: return null
        
        return before + replacement + after
    }

    fun validateCorrection(text: String, correction: Correction): Boolean {
        if (correction.startOffset < 0 || correction.endOffset > text.length) return false
        if (correction.startOffset >= correction.endOffset) return false
        
        val currentText = text.substring(correction.startOffset, correction.endOffset)
        return currentText == correction.originalText || currentText.equals(correction.originalText, ignoreCase = true)
    }

    fun applyMultipleCorrections(text: String, corrections: List<Correction>): String {
        // Sort by offset descending to apply from end to start (preserves earlier offsets)
        val sorted = corrections
            .filter { it.suggestedReplacement != null && it.isSafeToApply }
            .sortedByDescending { it.startOffset }
        
        var result = text
        for (correction in sorted) {
            val applied = applyCorrection(result, correction)
            if (applied != null) {
                result = applied
            }
        }
        return result
    }

    fun adjustOffsetsAfterReplacement(
        corrections: List<Correction>,
        appliedCorrection: Correction
    ): List<Correction> {
        val replacement = appliedCorrection.suggestedReplacement ?: return corrections
        val lengthDiff = replacement.length - appliedCorrection.originalText.length
        
        return corrections.mapNotNull { correction ->
            when {
                // Same correction - remove it
                correction.id == appliedCorrection.id -> null
                // Before the applied correction - no change
                correction.endOffset <= appliedCorrection.startOffset -> correction
                // After the applied correction - shift offsets
                correction.startOffset >= appliedCorrection.endOffset -> correction.copy(
                    startOffset = correction.startOffset + lengthDiff,
                    endOffset = correction.endOffset + lengthDiff
                )
                // Overlapping - invalidate
                else -> null
            }
        }
    }
}
