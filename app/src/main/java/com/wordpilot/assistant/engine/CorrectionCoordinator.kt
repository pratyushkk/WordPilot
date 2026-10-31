package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.gector.GectorOnnxEngine
import com.wordpilot.assistant.engine.impl.RuleBasedGrammarEngine
import com.wordpilot.assistant.engine.impl.TrieSpellEngine
import com.wordpilot.assistant.engine.offline.model.AnalysisOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class CorrectionCoordinator(
    private val spellEngine: SpellEngine,
    private val grammarEngine: GrammarEngine,
    private val gectorEngine: GectorOnnxEngine? = null
) {
    suspend fun checkText(text: String): List<Correction> = coroutineScope {
        if (text.isBlank()) return@coroutineScope emptyList()

        val gectorJob = async {
            if (gectorEngine != null && gectorEngine.isAvailable()) {
                try { gectorEngine.check(text) } catch (e: Exception) { emptyList() }
            } else {
                emptyList()
            }
        }

        // Fast path: if both engines share the same offline facade, run single-pass analysis
        val baseCorrections = if (spellEngine is TrieSpellEngine && grammarEngine is RuleBasedGrammarEngine && spellEngine.facade === grammarEngine.facade) {
            spellEngine.facade.analyze(
                rawText = text,
                options = AnalysisOptions(
                    includeSpelling = spellEngine.isAvailable(),
                    includeGrammar = grammarEngine.isAvailable()
                )
            )
        } else {
            val spellJob = async { 
                try { spellEngine.check(text) } catch (e: Exception) { emptyList() }
            }
            val grammarJob = async { 
                try { grammarEngine.check(text) } catch (e: Exception) { emptyList() }
            }
            spellJob.await() + grammarJob.await()
        }

        val allCorrections = gectorJob.await() + baseCorrections
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
        if (text.isBlank() || sentenceStart >= sentenceEnd) return emptyList()

        val baseCorrections = if (spellEngine is TrieSpellEngine && grammarEngine is RuleBasedGrammarEngine && spellEngine.facade === grammarEngine.facade) {
            spellEngine.facade.checkSentenceSpan(
                rawText = text,
                sentenceStart = sentenceStart,
                sentenceEnd = sentenceEnd,
                options = AnalysisOptions(
                    includeSpelling = spellEngine.isAvailable(),
                    includeGrammar = grammarEngine.isAvailable()
                )
            )
        } else {
            val sentence = text.substring(sentenceStart, sentenceEnd.coerceAtMost(text.length))
            val corrections = checkText(sentence)
            corrections.map { it.copy(
                startOffset = it.startOffset + sentenceStart,
                endOffset = it.endOffset + sentenceStart
            )}
        }

        if (gectorEngine != null && gectorEngine.isAvailable()) {
            val sentence = text.substring(sentenceStart, sentenceEnd.coerceAtMost(text.length))
            val gectorResults = try {
                gectorEngine.check(sentence).map { it.copy(
                    startOffset = it.startOffset + sentenceStart,
                    endOffset = it.endOffset + sentenceStart
                )}
            } catch (e: Exception) {
                emptyList()
            }
            return resolveOverlaps(gectorResults + baseCorrections)
        }

        return resolveOverlaps(baseCorrections)
    }
}
