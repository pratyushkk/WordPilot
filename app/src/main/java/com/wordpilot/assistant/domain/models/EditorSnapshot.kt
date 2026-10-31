package com.wordpilot.assistant.domain.models

/**
 * Immutable snapshot of the text editor state at the moment analysis is requested.
 */
data class EditorSnapshot(
    val packageName: String?,
    val fieldIdentifier: String?,
    val fullText: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    val sentenceStart: Int,
    val sentenceEnd: Int,
    val sentenceText: String,
    val capturedAt: Long = System.currentTimeMillis(),
    val requestId: Long = 0L
) {
    val hasSelection: Boolean
        get() = selectionStart >= 0 && selectionEnd > selectionStart && selectionEnd <= fullText.length

    val cursorPosition: Int
        get() = when {
            selectionStart >= 0 -> selectionStart.coerceIn(0, fullText.length)
            else -> fullText.length
        }

    /**
     * Verifies that the given [currentText] matches this snapshot at the sentence boundary.
     */
    fun isSentenceIntact(currentText: String): Boolean {
        if (sentenceStart < 0 || sentenceEnd > currentText.length || sentenceStart >= sentenceEnd) {
            return false
        }
        val currentSub = currentText.substring(sentenceStart, sentenceEnd)
        return currentSub == sentenceText
    }
}
