package com.wordpilot.assistant.engine.normalization

import org.junit.Assert.*
import org.junit.Test

class OffsetValidatorTest {

    @Test
    fun `valid in-bounds range returns identical pair`() {
        val text = "Hello world"
        val result = OffsetValidator.validate(text, 0, 5)
        assertEquals(Pair(0, 5), result)
    }

    @Test
    fun `insertion position (start == end) is allowed`() {
        val text = "Hello world"
        val result = OffsetValidator.validate(text, 5, 5)
        assertEquals(Pair(5, 5), result)
    }

    @Test
    fun `negative offset returns null`() {
        val text = "Hello world"
        assertNull(OffsetValidator.validate(text, -1, 5))
        assertNull(OffsetValidator.validate(text, 0, -2))
    }

    @Test
    fun `start greater than text length returns null`() {
        val text = "Hello"
        assertNull(OffsetValidator.validate(text, 10, 15))
    }

    @Test
    fun `end greater than text length is clamped to text length`() {
        val text = "Hello world"
        val result = OffsetValidator.validate(text, 6, 20)
        assertEquals(Pair(6, 11), result)
    }

    @Test
    fun `byteOffsetToCharOffset handles ASCII text identically`() {
        val text = "Hello world"
        assertEquals(5, OffsetValidator.byteOffsetToCharOffset(text, 5))
    }

    @Test
    fun `byteOffsetToCharOffset handles multibyte UTF-8 characters`() {
        val text = "café" // 'é' is 2 bytes in UTF-8
        // Byte length is 5, char length is 4
        assertEquals(4, OffsetValidator.byteOffsetToCharOffset(text, 5))
    }
}
