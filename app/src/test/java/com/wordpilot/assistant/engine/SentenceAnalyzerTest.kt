package com.wordpilot.assistant.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SentenceAnalyzerTest {

    private lateinit var analyzer: SentenceAnalyzer

    @Before
    fun setUp() {
        analyzer = SentenceAnalyzer()
    }

    @Test
    fun `test split sentences correctly`() {
        val text = "Hello world. This is sentence two! And sentence three?"
        val sentences = analyzer.splitSentences(text)

        assertEquals(3, sentences.size)
        assertEquals("Hello world.", sentences[0].text)
        assertEquals("This is sentence two!", sentences[1].text)
        assertEquals("And sentence three?", sentences[2].text)
    }

    @Test
    fun `test abbreviations are not treated as sentence boundaries`() {
        val text = "Dr. Smith went to the hospital. He saw the patient."
        val sentences = analyzer.splitSentences(text)

        assertEquals(2, sentences.size)
        assertTrue(sentences[0].text.startsWith("Dr. Smith"))
    }

    @Test
    fun `test find current sentence by cursor position`() {
        val text = "First sentence here. Second sentence starts here."
        val sentence = analyzer.findCurrentSentence(text, 25)

        assertNotNull(sentence)
        assertTrue(sentence!!.text.contains("Second sentence"))
    }

    @Test
    fun `test word completion detection`() {
        val text = "The quick brown fox "
        // At position 19 ('x'), next char is ' ' (whitespace) -> word is complete
        assertTrue(analyzer.isWordComplete(text, 19))

        val incomplete = "The quick brown fox"
        // At end of string without space -> word is incomplete
        assertFalse(analyzer.isWordComplete(incomplete, 19))
    }
}
