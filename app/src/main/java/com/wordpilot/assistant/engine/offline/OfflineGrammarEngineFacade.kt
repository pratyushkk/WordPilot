package com.wordpilot.assistant.engine.offline

import android.content.Context
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.engine.offline.applier.CorrectionApplier
import com.wordpilot.assistant.engine.offline.dictionary.EnglishDictionary
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.morphology.MorphologyAnalyzer
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer
import com.wordpilot.assistant.engine.offline.pos.PartOfSpeechAnalyzer
import com.wordpilot.assistant.engine.offline.ranking.SuggestionRanker
import com.wordpilot.assistant.engine.offline.rules.GrammarRuleEngine
import com.wordpilot.assistant.engine.offline.segmenter.SentenceSegmenter
import com.wordpilot.assistant.engine.offline.spelling.SpellingChecker
import com.wordpilot.assistant.engine.offline.tokenizer.Tokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineGrammarEngineFacade(
    context: Context? = null
) {
    val normalizer = TextNormalizer()
    val segmenter = SentenceSegmenter()
    val tokenizer = Tokenizer()
    val morphology = MorphologyAnalyzer()
    val dictionary = EnglishDictionary(context, morphology)
    val posAnalyzer = PartOfSpeechAnalyzer(dictionary)
    val spellingChecker = SpellingChecker(dictionary)
    val ruleEngine = GrammarRuleEngine(morphology)
    val ranker = SuggestionRanker(dictionary)
    val applier = CorrectionApplier()

    suspend fun checkText(
        rawText: String,
        cursorOffset: Int? = null,
        minConfidence: Float = 0.70f
    ): List<Correction> = withContext(Dispatchers.Default) {
        if (rawText.isBlank()) return@withContext emptyList()

        // 1. Text normalization with offset mapping
        val normResult = normalizer.normalize(rawText)
        val normalizedText = normResult.normalizedText

        // 2. Sentence segmentation
        val sentenceSpans = segmenter.segment(normalizedText, normResult)
        if (sentenceSpans.isEmpty()) return@withContext emptyList()

        val allErrors = mutableListOf<GrammarError>()

        // 3. Process each sentence through the pipeline
        for (span in sentenceSpans) {
            val sentenceText = normalizedText.substring(span.start, span.end)
            val tokens = tokenizer.tokenize(sentenceText, normResult, baseOffset = span.start)
            val taggedTokens = posAnalyzer.tagSentence(tokens)

            val analyzedSentence = AnalyzedSentence(
                span = span,
                tokens = taggedTokens,
                fullText = normalizedText
            )

            // Spelling checks
            val spellErrors = spellingChecker.checkTokens(taggedTokens)
            allErrors.addAll(spellErrors)

            // Grammar and punctuation rule checks
            val grammarErrors = ruleEngine.checkSentence(analyzedSentence)
            allErrors.addAll(grammarErrors)
        }

        // 4. Rank, filter, and suppress
        ranker.rankAndFilter(allErrors, cursorOffset, minConfidence)
    }

    suspend fun checkSentenceSpan(
        rawText: String,
        sentenceStart: Int,
        sentenceEnd: Int,
        cursorOffset: Int? = null,
        minConfidence: Float = 0.70f
    ): List<Correction> = withContext(Dispatchers.Default) {
        if (rawText.isBlank() || sentenceStart >= sentenceEnd) return@withContext emptyList()

        val boundedStart = sentenceStart.coerceIn(0, rawText.length)
        val boundedEnd = sentenceEnd.coerceIn(boundedStart, rawText.length)
        val sentenceSubstring = rawText.substring(boundedStart, boundedEnd)

        val corrections = checkText(sentenceSubstring, cursorOffset?.let { it - boundedStart }, minConfidence)

        // Adjust offsets back to full raw document
        corrections.map { c ->
            c.copy(
                startOffset = c.startOffset + boundedStart,
                endOffset = c.endOffset + boundedStart
            )
        }
    }

    fun getSpellingSuggestions(word: String, maxResults: Int = 5): List<String> {
        return spellingChecker.getSuggestions(word, maxResults)
    }

    fun addCustomWord(word: String) {
        dictionary.addCustomWord(word)
    }

    fun removeCustomWord(word: String) {
        dictionary.removeCustomWord(word)
    }

    fun isKnownWord(word: String): Boolean {
        return dictionary.isKnownWord(word)
    }

    fun getSupportedRuleIds(): List<String> = ruleEngine.getSupportedRuleIds()

    fun setDisabledRules(ruleIds: Set<String>) {
        ruleEngine.setDisabledRules(ruleIds)
    }
}
