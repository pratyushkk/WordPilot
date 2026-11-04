package com.wordpilot.assistant.domain

import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SafeReplacementHelperTest {

    private lateinit var helper: SafeReplacementHelper

    @Before
    fun setUp() {
        helper = SafeReplacementHelper()
    }

    @Test
    fun `test valid correction replacement`() {
        val text = "I went went to market"
        val correction = Correction(
            startOffset = 2,
            endOffset = 11,
            originalText = "went went",
            suggestedReplacement = "went",
            category = CorrectionCategory.GRAMMAR,
            explanation = "Remove repetition",
            severity = CorrectionSeverity.ERROR,
            ruleId = "REPEATED_WORD"
        )

        val result = helper.applyCorrection(text, correction)
        assertEquals("I went to market", result)
    }

    @Test
    fun `test stale correction is rejected when text does not match`() {
        val text = "I walked to market"
        val correction = Correction(
            startOffset = 2,
            endOffset = 11,
            originalText = "went went",
            suggestedReplacement = "went",
            category = CorrectionCategory.GRAMMAR,
            explanation = "Remove repetition",
            severity = CorrectionSeverity.ERROR,
            ruleId = "REPEATED_WORD"
        )

        val isValid = helper.validateCorrection(text, correction)
        assertFalse("Stale correction should not validate", isValid)

        val result = helper.applyCorrection(text, correction)
        assertNull("Stale correction should not apply", result)
    }

    @Test
    fun `test multiple non-overlapping corrections applied from end to start`() {
        val text = "She go to school and i has a dog."
        val correction1 = Correction(
            startOffset = 4,
            endOffset = 6,
            originalText = "go",
            suggestedReplacement = "goes",
            category = CorrectionCategory.GRAMMAR,
            explanation = "Use goes",
            severity = CorrectionSeverity.ERROR,
            ruleId = "SUBJECT_VERB_AGREEMENT"
        )
        val correction2 = Correction(
            startOffset = 23,
            endOffset = 26,
            originalText = "has",
            suggestedReplacement = "have",
            category = CorrectionCategory.GRAMMAR,
            explanation = "Use have",
            severity = CorrectionSeverity.ERROR,
            ruleId = "SUBJECT_VERB_AGREEMENT"
        )

        val result = helper.applyMultipleCorrections(text, listOf(correction1, correction2))
        assertEquals("She goes to school and i have a dog.", result)
    }

    @Test
    fun `test offset adjustment after single replacement`() {
        val applied = Correction(
            id = "c1",
            startOffset = 4,
            endOffset = 6,
            originalText = "go",
            suggestedReplacement = "goes", // +2 chars
            category = CorrectionCategory.GRAMMAR,
            explanation = "",
            severity = CorrectionSeverity.ERROR,
            ruleId = "test"
        )
        val remaining = Correction(
            id = "c2",
            startOffset = 23,
            endOffset = 26,
            originalText = "has",
            suggestedReplacement = "have",
            category = CorrectionCategory.GRAMMAR,
            explanation = "",
            severity = CorrectionSeverity.ERROR,
            ruleId = "test"
        )

        val adjusted = helper.adjustOffsetsAfterReplacement(listOf(applied, remaining), applied)
        assertEquals(1, adjusted.size)
        // Offset was 23, shifted by +2 ('goes'.length - 'go'.length = 4 - 2 = 2) -> 25
        assertEquals(25, adjusted[0].startOffset)
        assertEquals(28, adjusted[0].endOffset)
    }
}
