package com.wordpilot.assistant.engine.normalization

/**
 * Paragraph-aware text chunker for the Sapling API.
 *
 * Sapling recommends chunking texts above ~4000 chars to avoid timeouts.
 * This chunker:
 * - Splits on paragraph boundaries (double newlines) preferentially
 * - Falls back to sentence boundaries if needed
 * - Preserves original character offsets via a mapping
 * - Never sends incomplete sentences unless forced
 */
object SaplingChunker {

    data class TextChunk(
        val text: String,
        val documentOffset: Int  // char offset of this chunk's start in the original document
    )

    fun chunk(text: String, maxCharsPerChunk: Int = 4000): List<TextChunk> {
        if (text.length <= maxCharsPerChunk) {
            return listOf(TextChunk(text, 0))
        }

        val chunks = mutableListOf<TextChunk>()
        var pos = 0

        while (pos < text.length) {
            val remaining = text.length - pos
            if (remaining <= maxCharsPerChunk) {
                chunks.add(TextChunk(text.substring(pos), pos))
                break
            }

            // Search for paragraph boundary within maxCharsPerChunk
            val end = (pos + maxCharsPerChunk).coerceAtMost(text.length)
            val window = text.substring(pos, end)

            // Try paragraph boundary first (double newline)
            val paraBreak = window.lastIndexOf("\n\n")
            if (paraBreak > 0) {
                val chunkEnd = pos + paraBreak + 2  // include the newlines
                chunks.add(TextChunk(text.substring(pos, chunkEnd), pos))
                pos = chunkEnd
                continue
            }

            // Try single newline boundary
            val lineBreak = window.lastIndexOf('\n')
            if (lineBreak > maxCharsPerChunk / 2) {
                val chunkEnd = pos + lineBreak + 1
                chunks.add(TextChunk(text.substring(pos, chunkEnd), pos))
                pos = chunkEnd
                continue
            }

            // Try sentence boundary ('. ' or '! ' or '? ')
            val sentenceEnd = findLastSentenceBoundary(window, maxCharsPerChunk / 2)
            if (sentenceEnd > 0) {
                val chunkEnd = pos + sentenceEnd
                chunks.add(TextChunk(text.substring(pos, chunkEnd), pos))
                pos = chunkEnd
                continue
            }

            // Last resort: word boundary
            val wordEnd = window.lastIndexOf(' ')
            if (wordEnd > maxCharsPerChunk / 2) {
                val chunkEnd = pos + wordEnd + 1
                chunks.add(TextChunk(text.substring(pos, chunkEnd), pos))
                pos = chunkEnd
                continue
            }

            // Hard cut
            chunks.add(TextChunk(text.substring(pos, end), pos))
            pos = end
        }

        return chunks
    }

    private fun findLastSentenceBoundary(text: String, minPos: Int): Int {
        var i = text.length - 1
        while (i > minPos) {
            val c = text[i]
            if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length && text[i + 1] == ' ') {
                return i + 2  // position after the space
            }
            i--
        }
        return -1
    }
}
