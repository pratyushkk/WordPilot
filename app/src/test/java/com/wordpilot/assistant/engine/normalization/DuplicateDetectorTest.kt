package com.wordpilot.assistant.engine.normalization

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import org.junit.Assert.*
import org.junit.Test

class DuplicateDetectorTest {

    private fun makeCorrection(
        start: Int,
        end: Int,
        original: String,
        replacement: String?,
        severity: CorrectionSeverity = CorrectionSeverity.WARNING
    ) = Correction(
        startOffset = start,
        endOffset = end,
        originalText = original,
        suggestedReplacement = replacement,
        category = CorrectionCategory.GRAMMAR,
        explanation = "test",
        severity = severity,
        ruleId = "TEST_RULE"
    )

    @Test
    fun `empty list returns empty list`() {
        assertTrue(DuplicateDetector.process(emptyList()).isEmpty())
    }

    @Test
    fun `single correction is preserved`() {
        val list = listOf(makeCorrection(0, 4, "Them", "They"))
        assertEquals(1, DuplicateDetector.process(list).size)
    }

    @Test
    fun `exact duplicates on same range with same replacement are removed`() {
        val c1 = makeCorrection(0, 4, "Them", "They")
        val c2 = makeCorrection(0, 4, "Them", "They")
        val result = DuplicateDetector.process(listOf(c1, c2))
        assertEquals(1, result.size)
    }

    @Test
    fun `overlapping edits resolve in favor of higher severity`() {
        // c1: warning at [0..5]
        val c1 = makeCorrection(0, 5, "theye", "they", severity = CorrectionSeverity.WARNING)
        // c2: error at [0..6] (overlaps)
        val c2 = makeCorrection(0, 6, "theye ", "they ", severity = CorrectionSeverity.ERROR)

        val result = DuplicateDetector.deduplicate(listOf(c1, c2))
        assertEquals(1, result.size)
        assertEquals(CorrectionSeverity.ERROR, result[0].severity)
    }

    @Test
    fun `non-overlapping adjacent edits are both preserved`() {
        val c1 = makeCorrection(0, 4, "Them", "They")
        val c2 = makeCorrection(5, 7, "is", "are")
        val result = DuplicateDetector.process(listOf(c1, c2))
        assertEquals(2, result.size)
    }
}
