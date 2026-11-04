package com.wordpilot.assistant.domain.correction

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.engine.CorrectionCoordinator
import com.wordpilot.assistant.engine.SentenceAnalyzer

class CorrectionUseCase(
    private val coordinator: CorrectionCoordinator,
    private val sentenceAnalyzer: SentenceAnalyzer,
    private val safeReplacementHelper: SafeReplacementHelper
) {
    suspend fun checkFullText(text: String): List<Correction> {
        return coordinator.checkText(text)
    }

    suspend fun checkCurrentSentence(text: String, cursorPosition: Int): List<Correction> {
        val sentence = sentenceAnalyzer.findCurrentSentence(text, cursorPosition)
            ?: return emptyList()
        return coordinator.checkSentence(text, sentence.start, sentence.end)
    }

    fun applyCorrection(text: String, correction: Correction): String? {
        return safeReplacementHelper.applyCorrection(text, correction)
    }

    fun validateCorrection(text: String, correction: Correction): Boolean {
        return safeReplacementHelper.validateCorrection(text, correction)
    }

    fun adjustRemainingCorrections(
        corrections: List<Correction>,
        applied: Correction
    ): List<Correction> {
        return safeReplacementHelper.adjustOffsetsAfterReplacement(corrections, applied)
    }
}
