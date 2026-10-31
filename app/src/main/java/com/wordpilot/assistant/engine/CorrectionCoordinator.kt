package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.gector.GectorOnnxEngine
import com.wordpilot.assistant.engine.impl.RuleBasedGrammarEngine
import com.wordpilot.assistant.engine.impl.TrieSpellEngine
import com.wordpilot.assistant.engine.offline.model.AnalysisOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class CorrectionCoordinator(
    private val spellEngine: SpellEngine,
    private val grammarEngine: GrammarEngine,
    private val gectorEngine: GectorOnnxEngine? = null,
    private val useGectorOnly: Boolean = (gectorEngine != null),
    private val sentenceAnalyzer: SentenceAnalyzer = SentenceAnalyzer()
) {
    suspend fun checkText(text: String): List<Correction> = coroutineScope {
        if (text.isBlank()) return@coroutineScope emptyList()

        if (useGectorOnly && gectorEngine != null && gectorEngine.isAvailable()) {
            val sentenceSpans = sentenceAnalyzer.splitSentences(text)
            val allResults = mutableListOf<Correction>()
            val spans = if (sentenceSpans.isNotEmpty()) sentenceSpans else listOf(SentenceSpan(0, text.length, text))

            for (span in spans) {
                try {
                    val rawSentence = span.text
                    val capPunctCorrections = checkCapitalizationAndPunctuation(rawSentence, listOf(SentenceSpan(0, rawSentence.length, rawSentence)))
                    
                    // Apply capitalization to help GECToR produce optimal grammar & agreement predictions
                    var normalizedForGector = rawSentence
                    for (c in capPunctCorrections.filter { it.category == CorrectionCategory.CAPITALIZATION }.sortedByDescending { it.startOffset }) {
                        val repl = c.suggestedReplacement ?: continue
                        if (c.originalText.length == repl.length) {
                            normalizedForGector = normalizedForGector.substring(0, c.startOffset) + repl + normalizedForGector.substring(c.endOffset)
                        }
                    }
                    if (!normalizedForGector.endsWith(".") && !normalizedForGector.endsWith("?") && !normalizedForGector.endsWith("!")) {
                        normalizedForGector = "$normalizedForGector."
                    }

                    val gectorCorrections = gectorEngine.check(normalizedForGector).mapNotNull {
                        // If correction is beyond rawSentence length (due to added period), adjust or skip
                        if (it.startOffset >= rawSentence.length) null
                        else it.copy(
                            startOffset = (it.startOffset + span.start).coerceAtMost(span.end),
                            endOffset = (it.endOffset + span.start).coerceAtMost(span.end)
                        )
                    }

                    allResults.addAll(gectorCorrections)
                    allResults.addAll(capPunctCorrections.map { it.copy(
                        startOffset = it.startOffset + span.start,
                        endOffset = it.endOffset + span.start
                    )})
                } catch (e: Exception) {
                    // Continue checking subsequent sentences
                }
            }

            return@coroutineScope resolveOverlaps(allResults)
        }

        if (useGectorOnly) {
            // Built-in offline grammar engine is excluded per user configuration
            return@coroutineScope emptyList()
        }

        // Fast path for test mocks where GECToR is null
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

        resolveOverlaps(baseCorrections)
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

    private fun checkCapitalizationAndPunctuation(rawText: String, sentenceSpans: List<SentenceSpan>): List<Correction> {
        val list = mutableListOf<Correction>()
        val spans = if (sentenceSpans.isNotEmpty()) sentenceSpans else listOf(SentenceSpan(0, rawText.length, rawText))

        for (span in spans) {
            val sentence = span.text
            if (sentence.isBlank()) continue

            // 1. Sentence-initial capitalization
            var firstWordStart = 0
            while (firstWordStart < sentence.length && sentence[firstWordStart].isWhitespace()) {
                firstWordStart++
            }
            if (firstWordStart < sentence.length && sentence[firstWordStart].isLowerCase()) {
                var firstWordEnd = firstWordStart
                while (firstWordEnd < sentence.length && sentence[firstWordEnd].isLetter()) {
                    firstWordEnd++
                }
                val word = sentence.substring(firstWordStart, firstWordEnd)
                val capitalized = word.replaceFirstChar { it.uppercaseChar() }
                list.add(
                    Correction(
                        id = UUID.randomUUID().toString(),
                        startOffset = span.start + firstWordStart,
                        endOffset = span.start + firstWordEnd,
                        originalText = word,
                        suggestedReplacement = capitalized,
                        category = CorrectionCategory.CAPITALIZATION,
                        explanation = "Capitalize the first word of the sentence",
                        severity = CorrectionSeverity.WARNING,
                        ruleId = "GECTOR_CAPITAL",
                        isSafeToApply = true
                    )
                )
            }

            // 2. Standalone pronoun "i" and contractions ("i'm", "i've", "i'll", "i'd")
            val pronounPattern = Regex("""\b(i)('m|'ve|'ll|'d)?\b""")
            for (m in pronounPattern.findAll(sentence)) {
                val fullMatch = m.value
                val capitalized = fullMatch.replaceFirstChar { it.uppercaseChar() }
                list.add(
                    Correction(
                        id = UUID.randomUUID().toString(),
                        startOffset = span.start + m.range.first,
                        endOffset = span.start + m.range.last + 1,
                        originalText = fullMatch,
                        suggestedReplacement = capitalized,
                        category = CorrectionCategory.CAPITALIZATION,
                        explanation = "Capitalize the pronoun 'I'",
                        severity = CorrectionSeverity.WARNING,
                        ruleId = "GECTOR_CAPITAL",
                        isSafeToApply = true
                    )
                )
            }

            // 3. Sentence-ending punctuation (period '.')
            val trimmed = sentence.trimEnd()
            if (trimmed.isNotEmpty() && trimmed.last().isLetterOrDigit()) {
                val words = trimmed.split("\\s+".toRegex())
                if (words.size >= 2) {
                    val lastWord = words.last()
                    val lastWordOffset = span.start + trimmed.lastIndexOf(lastWord)
                    list.add(
                        Correction(
                            id = UUID.randomUUID().toString(),
                            startOffset = lastWordOffset,
                            endOffset = lastWordOffset + lastWord.length,
                            originalText = lastWord,
                            suggestedReplacement = "$lastWord.",
                            category = CorrectionCategory.PUNCTUATION,
                            explanation = "Add a period at the end of the sentence",
                            severity = CorrectionSeverity.SUGGESTION,
                            ruleId = "GECTOR_PUNCTUATION",
                            isSafeToApply = true
                        )
                    )
                }
            }

            // 4. Missing comma after introductory transition adverbs
            val introPattern = Regex("""^(However|Therefore|Furthermore|Moreover|In addition|First|Finally)\s+([A-Za-z])""")
            val introMatch = introPattern.find(sentence)
            if (introMatch != null) {
                val introWord = introMatch.groupValues[1]
                val introStart = sentence.indexOf(introWord)
                if (introStart >= 0) {
                    list.add(
                        Correction(
                            id = UUID.randomUUID().toString(),
                            startOffset = span.start + introStart,
                            endOffset = span.start + introStart + introWord.length,
                            originalText = introWord,
                            suggestedReplacement = "$introWord,",
                            category = CorrectionCategory.PUNCTUATION,
                            explanation = "Add a comma after introductory word '$introWord'",
                            severity = CorrectionSeverity.SUGGESTION,
                            ruleId = "GECTOR_PUNCTUATION",
                            isSafeToApply = true
                        )
                    )
                }
            }
        }

        return list
    }

    private fun severityWeight(severity: CorrectionSeverity): Int = when (severity) {
        CorrectionSeverity.ERROR -> 3
        CorrectionSeverity.WARNING -> 2
        CorrectionSeverity.SUGGESTION -> 1
    }

    suspend fun checkSentence(text: String, sentenceStart: Int, sentenceEnd: Int): List<Correction> {
        if (text.isBlank() || sentenceStart >= sentenceEnd) return emptyList()

        if (useGectorOnly && gectorEngine != null && gectorEngine.isAvailable()) {
            val sentence = text.substring(sentenceStart, sentenceEnd.coerceAtMost(text.length))
            val gectorResults = try {
                gectorEngine.check(sentence).map { it.copy(
                    startOffset = it.startOffset + sentenceStart,
                    endOffset = it.endOffset + sentenceStart
                )}
            } catch (e: Exception) {
                emptyList()
            }
            return resolveOverlaps(gectorResults)
        }

        if (useGectorOnly) {
            // Built-in offline grammar engine is excluded
            return emptyList()
        }

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

        return resolveOverlaps(baseCorrections)
    }
}
