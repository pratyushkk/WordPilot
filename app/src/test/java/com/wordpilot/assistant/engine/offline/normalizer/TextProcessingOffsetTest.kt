package com.wordpilot.assistant.engine.offline.normalizer

import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.OfflineGrammarEngineFacade
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TextProcessingOffsetTest {

    private lateinit var normalizer: TextNormalizer
    private lateinit var facade: OfflineGrammarEngineFacade
    private lateinit var replacementHelper: SafeReplacementHelper

    @Before
    fun setUp() {
        normalizer = TextNormalizer()
        facade = OfflineGrammarEngineFacade(null)
        replacementHelper = SafeReplacementHelper()
    }

    @Test
    fun testSmartPunctuationAndDashesPreserveOriginalOffsets() {
        val raw = "“Don’t worry—it’s fine…”"
        val result = normalizer.normalize(raw)

        assertEquals("\"Don't worry-it's fine...\"", result.normalizedText)
        assertEquals(0, result.toOriginalOffset(0))
        assertEquals(raw.length, result.toOriginalOffset(result.normalizedText.length))

        // Check each character mapping round-trip
        for (i in 0 until result.normalizedText.length) {
            val origOffset = result.toOriginalOffset(i)
            assertTrue("origOffset must be within raw length", origOffset in 0 until raw.length)
        }
    }

    @Test
    fun testSurrogatePairEmojisPreserveOffsets() = runBlocking {
        val raw = "Hello 😊 She go to school 👍 everyday."
        val corrections = facade.checkText(raw)

        // Find the "She go" -> "goes" error
        val svError = corrections.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should detect subject-verb error despite emojis", svError)

        // Mandatory invariant
        assertTrue("startOffset >= 0", svError!!.startOffset >= 0)
        assertTrue("endOffset <= raw.length", svError.endOffset <= raw.length)
        val extracted = raw.substring(svError.startOffset, svError.endOffset)
        assertEquals("Substrings must match exactly", svError.originalText, extracted)
        assertEquals("go", svError.originalText)
        assertEquals("goes", svError.suggestedReplacement)
    }

    @Test
    fun testSafeReplacementHelperEnforcesInvariant() {
        val text = "She go to school."
        val validCorrection = Correction(
            id = UUID.randomUUID().toString(),
            startOffset = 4,
            endOffset = 6,
            originalText = "go",
            suggestedReplacement = "goes",
            category = CorrectionCategory.GRAMMAR,
            explanation = "Subject-verb agreement",
            severity = CorrectionSeverity.ERROR,
            ruleId = "SUBJECT_VERB_AGREEMENT",
            isSafeToApply = true
        )

        // 1. Valid application
        assertTrue(replacementHelper.validateCorrection(text, validCorrection))
        val replaced = replacementHelper.applyCorrection(text, validCorrection)
        assertEquals("She goes to school.", replaced)

        // 2. Stale correction (originalText mismatch)
        val staleCorrection = validCorrection.copy(originalText = "walks")
        assertFalse(replacementHelper.validateCorrection(text, staleCorrection))
        assertNull(replacementHelper.applyCorrection(text, staleCorrection))

        // 3. Out of bounds
        val outOfBounds = validCorrection.copy(startOffset = 50, endOffset = 55)
        assertFalse(replacementHelper.validateCorrection(text, outOfBounds))
        assertNull(replacementHelper.applyCorrection(text, outOfBounds))

        // 4. Inverted offsets
        val inverted = validCorrection.copy(startOffset = 6, endOffset = 4)
        assertFalse(replacementHelper.validateCorrection(text, inverted))
        assertNull(replacementHelper.applyCorrection(text, inverted))
    }

    @Test
    fun testSafeReplacementRejectsSplittingSurrogatePair() {
        val text = "Hi 😊 there" // '😊' is at indices [3, 5)
        val splitSurrogateCorrection = Correction(
            id = UUID.randomUUID().toString(),
            startOffset = 4, // Inside the surrogate pair!
            endOffset = 6,
            originalText = text.substring(4, 6),
            suggestedReplacement = "foo",
            category = CorrectionCategory.GRAMMAR,
            explanation = "Invalid surrogate test",
            severity = CorrectionSeverity.ERROR,
            ruleId = "TEST",
            isSafeToApply = true
        )

        assertFalse("Must reject correction that begins inside surrogate pair",
            replacementHelper.validateCorrection(text, splitSurrogateCorrection))
        assertNull(replacementHelper.applyCorrection(text, splitSurrogateCorrection))
    }

    @Test
    fun testMultipleReplacementsApplyWithoutDrift() {
        val text = "She have a apple."
        val c1 = Correction(
            id = "1",
            startOffset = 4,
            endOffset = 8,
            originalText = "have",
            suggestedReplacement = "has",
            category = CorrectionCategory.GRAMMAR,
            explanation = "SV test",
            severity = CorrectionSeverity.ERROR,
            ruleId = "SV",
            isSafeToApply = true
        )
        val c2 = Correction(
            id = "2",
            startOffset = 9,
            endOffset = 10,
            originalText = "a",
            suggestedReplacement = "an",
            category = CorrectionCategory.GRAMMAR,
            explanation = "Article test",
            severity = CorrectionSeverity.ERROR,
            ruleId = "ART",
            isSafeToApply = true
        )

        val result = replacementHelper.applyMultipleCorrections(text, listOf(c1, c2))
        assertEquals("She has an apple.", result)
    }
}
