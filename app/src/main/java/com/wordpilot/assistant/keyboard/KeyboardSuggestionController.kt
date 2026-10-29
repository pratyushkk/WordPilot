package com.wordpilot.assistant.keyboard

import android.view.inputmethod.InputConnection
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.engine.SentenceSpan
import com.wordpilot.assistant.privacy.SensitiveInputGuard
import com.wordpilot.assistant.utils.Debouncer
import com.wordpilot.assistant.utils.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeyboardSuggestionController(
    private val scope: CoroutineScope
) {
    private val app = WordPilotApp.instance
    private val correctionCoordinator = app.correctionCoordinator
    private val spellEngine = app.spellEngine
    private val sentenceAnalyzer = app.sentenceAnalyzer
    private val debouncer = Debouncer(scope, 350L)

    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    private var currentInputConnection: InputConnection? = null
    private var lastAnalyzedText: String = ""

    fun setInputConnection(ic: InputConnection?) {
        currentInputConnection = ic
    }

    fun setSensitiveField(isSensitive: Boolean) {
        _uiState.update { 
            it.copy(
                isSensitiveField = isSensitive,
                activeCorrection = if (isSensitive) null else it.activeCorrection,
                wordSuggestions = if (isSensitive) emptyList() else it.wordSuggestions
            )
        }
    }

    fun onTextChanged(textBeforeCursor: CharSequence?, textAfterCursor: CharSequence?) {
        if (_uiState.value.isSensitiveField) return

        val textBefore = textBeforeCursor?.toString() ?: ""
        val textAfter = textAfterCursor?.toString() ?: ""
        val fullRecentText = textBefore + textAfter

        // Extract current word being typed (for prediction)
        val currentWord = extractCurrentWord(textBefore)
        updateWordPredictions(currentWord)

        // Only trigger grammar/spelling check on completed words or punctuation
        val isWordBoundary = textBefore.isNotEmpty() && 
            (textBefore.last().isWhitespace() || textBefore.last() in setOf('.', ',', '!', '?', ';', ':'))

        if (isWordBoundary && fullRecentText != lastAnalyzedText) {
            debouncer.debounce {
                analyzeText(textBefore, textAfter)
            }
        }
    }

    private fun extractCurrentWord(textBefore: String): String {
        if (textBefore.isEmpty()) return ""
        val lastSpace = textBefore.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '.', ',', '!', '?', ';', ':'))
        return if (lastSpace == -1) textBefore else textBefore.substring(lastSpace + 1)
    }

    private fun updateWordPredictions(prefix: String) {
        if (prefix.length < 2 || TextUtils.shouldSkipWord(prefix)) {
            _uiState.update { it.copy(wordSuggestions = emptyList()) }
            return
        }

        scope.launch(Dispatchers.Default) {
            val suggestions = spellEngine.getSuggestions(prefix, 3)
            _uiState.update { it.copy(wordSuggestions = suggestions) }
        }
    }

    private suspend fun analyzeText(textBefore: String, textAfter: String) = withContext(Dispatchers.Default) {
        val fullText = textBefore + textAfter
        if (fullText.isBlank()) {
            _uiState.update { it.copy(activeCorrection = null, isAnalyzing = false) }
            return@withContext
        }

        _uiState.update { it.copy(isAnalyzing = true) }
        lastAnalyzedText = fullText

        try {
            // Find current sentence around the cursor
            val cursorOffset = textBefore.length
            val sentence = sentenceAnalyzer.findCurrentSentence(fullText, cursorOffset)
                ?: SentenceSpan(0, fullText.length, fullText)

            val corrections = correctionCoordinator.checkSentence(
                fullText,
                sentence.start,
                sentence.end
            )

            // Filter out ignored rules & personal dictionary words
            val validCorrections = corrections.filter { correction ->
                val isIgnored = app.dictionaryRepository.isRuleIgnored(correction.ruleId, correction.originalText)
                val inDict = if (correction.category == CorrectionCategory.SPELLING) {
                    app.dictionaryRepository.wordExists(correction.originalText.lowercase())
                } else false
                !isIgnored && !inDict
            }

            // Pick the most relevant correction (closest to cursor or most severe)
            val topCorrection = validCorrections.minByOrNull { 
                kotlin.math.abs(it.startOffset - cursorOffset) 
            }

            _uiState.update { 
                it.copy(
                    activeCorrection = topCorrection,
                    isAnalyzing = false
                ) 
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }

    /**
     * Safely applies the correction to the active InputConnection.
     */
    fun acceptCorrection(correction: Correction): Boolean {
        val ic = currentInputConnection ?: return false
        val replacement = correction.suggestedReplacement ?: return false

        // Get text before cursor to locate original word
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val originalText = correction.originalText

        // Check if the word ends right before cursor (with optional trailing space)
        val trimmedBefore = textBefore.trimEnd()
        val trailingSpaces = textBefore.length - trimmedBefore.length

        if (trimmedBefore.endsWith(originalText, ignoreCase = true)) {
            val deleteCount = originalText.length + trailingSpaces
            ic.deleteSurroundingText(deleteCount, 0)
            ic.commitText(replacement + " ".repeat(trailingSpaces), 1)
            dismissCorrection()
            return true
        }

        // Search within the last 50 chars if user moved cursor slightly
        val lastIdx = textBefore.lastIndexOf(originalText, ignoreCase = true)
        if (lastIdx != -1) {
            val charsFromEnd = textBefore.length - lastIdx
            // Delete up to the word and re-insert surrounding
            val afterWord = textBefore.substring(lastIdx + originalText.length)
            ic.deleteSurroundingText(charsFromEnd, 0)
            ic.commitText(replacement + afterWord, 1)
            dismissCorrection()
            return true
        }

        return false
    }

    fun dismissCorrection() {
        _uiState.update { it.copy(activeCorrection = null, isCorrectionCardExpanded = false) }
    }

    fun toggleCardExpanded() {
        _uiState.update { it.copy(isCorrectionCardExpanded = !it.isCorrectionCardExpanded) }
    }

    fun addToDictionary(correction: Correction) {
        scope.launch {
            val word = correction.originalText.lowercase().trim()
            app.dictionaryRepository.addWord(word)
            app.spellEngine.addWord(word)
            dismissCorrection()
        }
    }

    fun ignoreRule(correction: Correction) {
        scope.launch {
            app.dictionaryRepository.ignoreRule(correction.ruleId, correction.originalText)
            dismissCorrection()
        }
    }

    fun setMode(mode: KeyboardMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun toggleShift() {
        _uiState.update { it.copy(shiftState = it.shiftState.next()) }
    }

    fun resetShiftAfterCharacter() {
        if (_uiState.value.shiftState == ShiftState.SHIFTED) {
            _uiState.update { it.copy(shiftState = ShiftState.OFF) }
        }
    }
}
