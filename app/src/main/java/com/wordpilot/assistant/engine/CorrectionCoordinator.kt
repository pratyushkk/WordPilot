package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class CorrectionCoordinator(
    private val spellEngine: SpellEngine,
    private val grammarEngine: GrammarEngine
) {
    suspend fun checkText(text: String): List<Correction> = coroutineScope {
        if (text.isBlank()) return@coroutineScope emptyList()

        val spellJob = async { 
            try { spellEngine.check(text) } catch (e: Exception) { emptyList() }
        }
        val grammarJob = async { 
            try { grammarEngine.check(text) } catch (e: Exception) { emptyList() }
        }

        val allCorrections = spellJob.await() + grammarJob.await()
        
        // Remove overlapping corrections, preferring higher severity
        resolveOverlaps(allCorrections)
    }

    private fun resolveOverlaps(corrections: List<Correction>): List<Correction> {
        if (corrections.size <= 1) return corrections

        val sorted = corrections.sortedWith(
            compareBy<Correction> { it.startOffset }
                .thenByDescending { severityWeight(it.severity) }
        )

        val result = mutableListOf<Correction>()
        var lastEnd = -1

        for (correction in sorted) {
            if (correction.startOffset >= lastEnd) {
                result.add(correction)
                lastEnd = correction.endOffset
            }
        }

        return result
    }

    private fun severityWeight(severity: CorrectionSeverity): Int = when (severity) {
        CorrectionSeverity.ERROR -> 3
        CorrectionSeverity.WARNING -> 2
        CorrectionSeverity.SUGGESTION -> 1
    }

    suspend fun checkSentence(text: String, sentenceStart: Int, sentenceEnd: Int): List<Correction> {
        val sentence = text.substring(sentenceStart, sentenceEnd.coerceAtMost(text.length))
        val corrections = checkText(sentence)
        // Adjust offsets to absolute positions
        return corrections.map { it.copy(
            startOffset = it.startOffset + sentenceStart,
            endOffset = it.endOffset + sentenceStart
        )}
    }
}
