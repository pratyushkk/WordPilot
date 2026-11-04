package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.engine.impl.RuleBasedGrammarEngine
import com.wordpilot.assistant.engine.impl.TrieSpellEngine
import com.wordpilot.assistant.engine.offline.segmenter.SentenceSegmenter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProofreadingRegressionTest {

    private lateinit var spellEngine: TrieSpellEngine
    private lateinit var grammarEngine: RuleBasedGrammarEngine
    private lateinit var coordinator: CorrectionCoordinator
    private lateinit var segmenter: SentenceSegmenter

    @Before
    fun setUp() {
        spellEngine = TrieSpellEngine(null)
        grammarEngine = RuleBasedGrammarEngine(null)
        coordinator = CorrectionCoordinator(spellEngine, grammarEngine)
        segmenter = SentenceSegmenter()
    }

    /**
     * Mandatory Invariant Check:
     * For EVERY correction emitted by the engine,
     * startOffset and endOffset must be valid indices in originalText,
     * startOffset < endOffset, and the substring must match correction.originalText exactly.
     */
    private fun assertValidSpans(originalText: String, corrections: List<Correction>) {
        for (correction in corrections) {
            assertTrue(
                "startOffset (${correction.startOffset}) must be >= 0",
                correction.startOffset >= 0
            )
            assertTrue(
                "endOffset (${correction.endOffset}) must be <= text.length (${originalText.length})",
                correction.endOffset <= originalText.length
            )
            assertTrue(
                "startOffset (${correction.startOffset}) must be < endOffset (${correction.endOffset})",
                correction.startOffset < correction.endOffset
            )
            val extracted = originalText.substring(correction.startOffset, correction.endOffset)
            assertEquals(
                "Extracted substring at [${correction.startOffset}, ${correction.endOffset}) must equal correction.originalText",
                correction.originalText,
                extracted
            )
        }
    }

    // ==========================================
    // 1. SPELLING REGRESSION
    // ==========================================

    @Test
    fun testCommonSpellingTypos() = runBlocking {
        val text = "I recieved your message and it is definately seperate."
        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val recieved = corrections.find { it.originalText == "recieved" }
        assertNotNull("Should detect 'recieved'", recieved)
        assertEquals("received", recieved?.suggestedReplacement)

        val definately = corrections.find { it.originalText == "definately" }
        assertNotNull("Should detect 'definately'", definately)
        assertEquals("definitely", definately?.suggestedReplacement)

        val seperate = corrections.find { it.originalText == "seperate" }
        assertNotNull("Should detect 'seperate'", seperate)
        assertEquals("separate", seperate?.suggestedReplacement)
    }

    @Test
    fun testCustomDictionaryWhitelist() = runBlocking {
        val customWord = "MotorolaEdge"
        val text = "I purchased a new MotorolaEdge today."

        // Initially unknown
        assertFalse(spellEngine.isKnownWord(customWord))

        // Whitelist the word
        spellEngine.addWord(customWord)
        assertTrue(spellEngine.isKnownWord(customWord))

        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val flagged = corrections.find { it.originalText.equals(customWord, ignoreCase = true) }
        assertNull("Custom dictionary word should not be flagged", flagged)
    }

    @Test
    fun testMissingApostropheContractions() = runBlocking {
        val text = "dont worry if you cant do it."
        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val dont = corrections.find { it.originalText == "dont" }
        assertNotNull("Should detect 'dont'", dont)
        assertEquals("don't", dont?.suggestedReplacement)

        val cant = corrections.find { it.originalText == "cant" }
        assertNotNull("Should detect 'cant'", cant)
        assertEquals("can't", cant?.suggestedReplacement)
    }

    // ==========================================
    // 2. GRAMMAR REGRESSION
    // ==========================================

    @Test
    fun testSubjectVerbAgreement() = runBlocking {
        val text = "She go to school every day."
        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val sv = corrections.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should detect 'She go'", sv)
        assertEquals("goes", sv?.suggestedReplacement)
    }

    @Test
    fun testSubjectVerbAgreementWithHas() = runBlocking {
        val text = "They has arrived safely."
        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val sv = corrections.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should detect 'They has'", sv)
        assertEquals("have", sv?.suggestedReplacement)
    }

    @Test
    fun testArticleErrors() = runBlocking {
        val text = "I have a apple and an university degree."
        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val appleArt = corrections.find { it.originalText == "a" && it.ruleId == "ARTICLE_A_AN" }
        assertNotNull("Should flag 'a apple'", appleArt)
        assertEquals("an", appleArt?.suggestedReplacement)

        val uniArt = corrections.find { it.originalText == "an" && it.ruleId == "ARTICLE_A_AN" }
        assertNotNull("Should flag 'an university'", uniArt)
        assertEquals("a", uniArt?.suggestedReplacement)
    }

    @Test
    fun testRepeatedWords() = runBlocking {
        val text = "I really really like this feature."
        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val rep = corrections.find { it.ruleId == "REPEATED_WORD" }
        assertNotNull("Should detect repeated 'really'", rep)
    }

    @Test
    fun testPunctuationSpacing() = runBlocking {
        val text = "The weather was good,but cold."
        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val punct = corrections.find { it.ruleId.startsWith("PUNCTUATION_SPACING") }
        assertNotNull("Should flag missing space after comma", punct)
    }

    // ==========================================
    // 3. SEGMENTATION & CAPITALIZATION
    // ==========================================

    @Test
    fun testSentenceSegmenterPreservesHonorificsAndDecimals() {
        val text = "Dr. Smith bought 3.14 lbs of apples at 10 a.m. He was very happy."
        val sentences = segmenter.segment(text)

        // Must not split on "Dr." or "3.14"
        assertEquals(2, sentences.size)
        assertTrue("First sentence must start with 'Dr. Smith'", sentences[0].text.startsWith("Dr. Smith"))
        assertTrue("First sentence must contain '3.14'", sentences[0].text.contains("3.14"))
        assertEquals("He was very happy.", sentences[1].text)
    }

    @Test
    fun testSentenceCapitalizationAndPronounI() = runBlocking {
        val text = "this is simple. yesterday i was there."
        val corrections = coordinator.checkText(text)
        assertValidSpans(text, corrections)

        val startCap = corrections.find { it.startOffset == 0 && it.category == CorrectionCategory.CAPITALIZATION }
        assertNotNull("Should capitalize sentence start", startCap)
        assertEquals("T", startCap?.suggestedReplacement)

        val pronounI = corrections.find { it.originalText == "i" }
        assertNotNull("Should capitalize pronoun 'i'", pronounI)
        assertEquals("I", pronounI?.suggestedReplacement)
    }

    // ==========================================
    // 4. COORDINATOR MULTI-SENTENCE CHECKING
    // ==========================================

    @Test
    fun testMultiSentenceCheckSentenceMaintainsAbsoluteOffsets() = runBlocking {
        val text = "She go home. They has arrived."
        // Sentence 1: "She go home." [0, 12)
        // Sentence 2: "They has arrived." [13, 30)
        val s1Corrections = coordinator.checkSentence(text, 0, 12)
        assertValidSpans(text, s1Corrections)
        assertEquals("She go", text.substring(0, 6))

        val s2Corrections = coordinator.checkSentence(text, 13, 30)
        assertValidSpans(text, s2Corrections)
        val hasCorrection = s2Corrections.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should detect 'has' in second sentence", hasCorrection)
        assertEquals("They has", text.substring(13, 21))
        assertEquals(18, hasCorrection?.startOffset)
        assertEquals(21, hasCorrection?.endOffset)
        assertEquals("has", hasCorrection?.originalText)
        assertEquals("have", hasCorrection?.suggestedReplacement)
    }
}
