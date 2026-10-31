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
        if (correction.startOffset > correction.endOffset) return false
        
        // Prevent severing UTF-16 surrogate pairs
        if (correction.startOffset > 0 && Character.isLowSurrogate(text[correction.startOffset]) && Character.isHighSurrogate(text[correction.startOffset - 1])) {
            return false
        }
        if (correction.endOffset < text.length && Character.isHighSurrogate(text[correction.endOffset - 1]) && Character.isLowSurrogate(text[correction.endOffset])) {
            return false
        }

        if (correction.startOffset == correction.endOffset) {
            return correction.originalText.isEmpty()
        }

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

    /**
     * Safely replaces a sentence using an immutable snapshot.
     * Returns null if the snapshot is stale or the sentence has been modified by the user.
     */
    fun replaceSentenceWithSnapshot(
        currentText: String,
        snapshot: com.wordpilot.assistant.domain.models.EditorSnapshot,
        newSentence: String
    ): String? {
        if (!snapshot.isSentenceIntact(currentText)) {
            return null
        }
        val before = currentText.substring(0, snapshot.sentenceStart)
        val after = currentText.substring(snapshot.sentenceEnd)
        return before + newSentence + after
    }

    /**
     * Inserts text at the specified cursor position or replaces the selected range,
     * cleanly handling spacing and preserving text before and after.
     */
    fun insertAtCursor(
        currentText: String,
        textToInsert: String,
        selectionStart: Int,
        selectionEnd: Int = selectionStart
    ): Pair<String, Int> {
        val cleanInsert = textToInsert.trim()
        if (cleanInsert.isEmpty()) return Pair(currentText, selectionStart.coerceIn(0, currentText.length))

        val start = selectionStart.coerceIn(0, currentText.length)
        val end = selectionEnd.coerceIn(start, currentText.length)

        val before = currentText.substring(0, start)
        val after = currentText.substring(end)

        val needsLeadingSpace = before.isNotEmpty() && !before.last().isWhitespace() && !before.endsWith("\n")
        val needsTrailingSpace = after.isNotEmpty() && !after.first().isWhitespace() && !after.startsWith("\n")

        val builder = StringBuilder()
        builder.append(before)
        if (needsLeadingSpace) builder.append(" ")
        builder.append(cleanInsert)
        if (needsTrailingSpace) builder.append(" ")
        val newCursor = builder.length
        builder.append(after)

        return Pair(builder.toString(), newCursor)
    }
}
