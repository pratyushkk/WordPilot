package com.wordpilot.assistant.engine.normalization

import org.junit.Assert.*
import org.junit.Test

class SaplingChunkerTest {

    @Test
    fun `short text under limit returns single chunk with offset 0`() {
        val text = "This is a short sentence."
        val chunks = SaplingChunker.chunk(text, maxCharsPerChunk = 100)
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0].text)
        assertEquals(0, chunks[0].documentOffset)
    }

    @Test
    fun `chunks on paragraph boundary when available`() {
        val p1 = "First paragraph content."
        val p2 = "Second paragraph content."
        val full = "$p1\n\n$p2"

        val chunks = SaplingChunker.chunk(full, maxCharsPerChunk = 30)
        assertTrue(chunks.size >= 2)
        assertEquals(0, chunks[0].documentOffset)
        assertEquals(p1 + "\n\n", chunks[0].text)
        assertEquals(p1.length + 2, chunks[1].documentOffset)
        assertEquals(p2, chunks[1].text)
    }

    @Test
    fun `chunks on sentence boundary when no paragraph break`() {
        val s1 = "This is sentence one. "
        val s2 = "This is sentence two."
        val full = s1 + s2

        val chunks = SaplingChunker.chunk(full, maxCharsPerChunk = 25)
        assertTrue(chunks.size >= 2)
        assertEquals(0, chunks[0].documentOffset)
        assertEquals(s1, chunks[0].text)
        assertEquals(s1.length, chunks[1].documentOffset)
    }

    @Test
    fun `chunk concatenation reconstructs full original text`() {
        val full = (1..10).joinToString("\n\n") { "Paragraph $it contains some text to test chunking." }
        val chunks = SaplingChunker.chunk(full, maxCharsPerChunk = 80)

        val reconstructed = chunks.joinToString("") { it.text }
        assertEquals(full, reconstructed)

        // Verify document offsets are strictly monotonic and match reconstructed positions
        var expectedOffset = 0
        for (chunk in chunks) {
            assertEquals(expectedOffset, chunk.documentOffset)
            expectedOffset += chunk.text.length
        }
    }
}
