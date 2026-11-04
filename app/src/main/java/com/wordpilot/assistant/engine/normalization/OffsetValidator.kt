package com.wordpilot.assistant.engine.normalization

object OffsetValidator {
    /**
     * Validates that start/end offsets are within [0, text.length] and start <= end.
     * Android uses UTF-16 code-unit indexing (same as Kotlin String.length).
     * Returns a clamped, valid range or null if the range is completely invalid.
     */
    fun validate(text: String, start: Int, end: Int): Pair<Int, Int>? {
        if (start < 0 || end < 0) return null
        if (start > text.length) return null
        val clampedEnd = end.coerceAtMost(text.length)
        val clampedStart = start.coerceAtMost(clampedEnd)
        // For insertions: start == end is allowed (insert at position)
        return Pair(clampedStart, clampedEnd)
    }

    /**
     * Adjusts a byte-based offset to UTF-16 code-unit offset.
     * Most providers (Sapling, LT) operate on UTF-16/char offsets already for English.
     * This helper exists for providers that may return byte offsets on non-ASCII text.
     */
    fun byteOffsetToCharOffset(text: String, byteOffset: Int): Int {
        if (byteOffset <= 0) return 0
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (byteOffset >= bytes.size) return text.length
        // Rebuild the char offset by counting code units
        var charCount = 0
        var byteCount = 0
        for (ch in text) {
            if (byteCount >= byteOffset) break
            byteCount += ch.toString().toByteArray(Charsets.UTF_8).size
            charCount++
        }
        return charCount.coerceAtMost(text.length)
    }
}
