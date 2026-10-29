package com.wordpilot.assistant.keyboard

import android.view.inputmethod.InputConnection
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.engine.SentenceSpan
import com.wordpilot.assistant.privacy.SensitiveInputGuard
import com.wordpilot.assistant.utils.Debouncer
import com.wordpilot.assistant.utils.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val rewriteCoordinator = app.rewriteCoordinator
    private val debouncer = Debouncer(scope, 350L)

    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    private var currentInputConnection: InputConnection? = null
    private var lastAnalyzedText: String = ""
    private var assistantRewriteJob: Job? = null

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
        if (_uiState.value.mode == KeyboardMode.EXPANDED_ASSISTANT) return

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
            val cursorOffset = textBefore.length
            val sentence = sentenceAnalyzer.findCurrentSentence(fullText, cursorOffset)
                ?: SentenceSpan(0, fullText.length, fullText)

            val corrections = correctionCoordinator.checkSentence(
                fullText,
                sentence.start,
                sentence.end
            )

            val validCorrections = corrections.filter { correction ->
                val isIgnored = app.dictionaryRepository.isRuleIgnored(correction.ruleId, correction.originalText)
                val inDict = if (correction.category == CorrectionCategory.SPELLING) {
                    app.dictionaryRepository.wordExists(correction.originalText.lowercase())
                } else false
                !isIgnored && !inDict
            }

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
     * Safely applies the single floating pill correction to the active InputConnection.
     */
    fun acceptCorrection(correction: Correction): Boolean {
        val ic = currentInputConnection ?: return false
        val replacement = correction.suggestedReplacement ?: return false

        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val originalText = correction.originalText

        val trimmedBefore = textBefore.trimEnd()
        val trailingSpaces = textBefore.length - trimmedBefore.length

        if (trimmedBefore.endsWith(originalText, ignoreCase = true)) {
            val deleteCount = originalText.length + trailingSpaces
            ic.deleteSurroundingText(deleteCount, 0)
            ic.commitText(replacement + " ".repeat(trailingSpaces), 1)
            dismissCorrection()
            return true
        }

        val lastIdx = textBefore.lastIndexOf(originalText, ignoreCase = true)
        if (lastIdx != -1) {
            val charsFromEnd = textBefore.length - lastIdx
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

    // =========================================================================
    // EXPANDED ASSISTANT LOGIC
    // =========================================================================

    /**
     * Captures current sentence / selected text and transforms keyboard window into Expanded Assistant.
     */
    fun openExpandedAssistant() {
        val ic = currentInputConnection
        var capturedText = ""
        var sourceType = AssistantSourceType.CURRENT_SENTENCE
        var startOffset = 0
        var endOffset = 0
        var cursorOffset = 0

        val selectedText = ic?.getSelectedText(0)?.toString()
        if (!selectedText.isNullOrBlank()) {
            capturedText = selectedText.trim()
            sourceType = AssistantSourceType.SELECTED_TEXT
        } else {
            val before = ic?.getTextBeforeCursor(1000, 0)?.toString() ?: ""
            val after = ic?.getTextAfterCursor(500, 0)?.toString() ?: ""
            val fullText = before + after
            cursorOffset = before.length

            if (fullText.isNotBlank()) {
                val span = sentenceAnalyzer.findCurrentSentence(fullText, cursorOffset)
                if (span != null) {
                    capturedText = span.text.trim()
                    startOffset = span.start
                    endOffset = span.end
                } else {
                    capturedText = fullText.trim()
                    startOffset = 0
                    endOffset = fullText.length
                }
            } else {
                sourceType = AssistantSourceType.MANUAL_INPUT
            }
        }

        val initialSession = AssistantSessionState(
            originalText = capturedText,
            sourceType = sourceType,
            sentenceStartOffset = startOffset,
            sentenceEndOffset = endOffset,
            cursorOffset = cursorOffset,
            currentEditText = capturedText,
            rewrittenText = "",
            selectedStyle = RewriteStyle.FIX_GRAMMAR,
            selectedEngine = rewriteCoordinator.selectedEngine.value,
            isProcessing = false
        )

        _uiState.update {
            it.copy(
                mode = KeyboardMode.EXPANDED_ASSISTANT,
                assistantSession = initialSession
            )
        }

        // Auto-run initial rewrite if text is present
        if (capturedText.isNotBlank()) {
            executeAssistantRewrite()
        }
    }

    fun closeExpandedAssistant() {
        assistantRewriteJob?.cancel()
        _uiState.update {
            it.copy(
                mode = KeyboardMode.QWERTY,
                assistantSession = null
            )
        }
    }

    fun updateAssistantSourceText(newText: String) {
        _uiState.update { state ->
            val session = state.assistantSession ?: return@update state
            state.copy(
                assistantSession = session.copy(currentEditText = newText)
            )
        }
    }

    fun selectAssistantStyle(style: RewriteStyle) {
        _uiState.update { state ->
            val session = state.assistantSession ?: return@update state
            state.copy(
                assistantSession = session.copy(selectedStyle = style)
            )
        }
        executeAssistantRewrite()
    }

    fun updateAssistantCustomInstruction(instruction: String) {
        _uiState.update { state ->
            val session = state.assistantSession ?: return@update state
            state.copy(
                assistantSession = session.copy(customInstruction = instruction)
            )
        }
    }

    fun selectAssistantEngine(engine: AIEngineType) {
        rewriteCoordinator.setEngineType(engine)
        _uiState.update { state ->
            val session = state.assistantSession ?: return@update state
            state.copy(
                assistantSession = session.copy(selectedEngine = engine)
            )
        }
        executeAssistantRewrite()
    }

    fun executeAssistantRewrite() {
        val currentSession = _uiState.value.assistantSession ?: return
        val textToProcess = currentSession.currentEditText.ifBlank { currentSession.originalText }
        if (textToProcess.isBlank()) return

        assistantRewriteJob?.cancel()
        _uiState.update { state ->
            val session = state.assistantSession ?: return@update state
            state.copy(
                assistantSession = session.copy(
                    isProcessing = true,
                    errorMessage = null
                )
            )
        }

        assistantRewriteJob = scope.launch(Dispatchers.Default) {
            val result = rewriteCoordinator.rewrite(
                text = textToProcess,
                style = currentSession.selectedStyle,
                customInstruction = if (currentSession.selectedStyle == RewriteStyle.CUSTOM)
                    currentSession.customInstruction else null,
                forcedEngine = currentSession.selectedEngine
            )

            _uiState.update { state ->
                val session = state.assistantSession ?: return@update state
                state.copy(
                    assistantSession = session.copy(
                        isProcessing = false,
                        rewrittenText = if (result.isSuccess) result.rewrittenText else session.rewrittenText,
                        errorMessage = result.errorMessage,
                        lastSuccess = result.isSuccess
                    )
                )
            }
        }
    }

    /**
     * Safely applies the assistant result to the active text field.
     */
    fun applyAssistantResult(finalText: String): Boolean {
        val ic = currentInputConnection ?: return false
        val session = _uiState.value.assistantSession ?: return false

        when (session.sourceType) {
            AssistantSourceType.SELECTED_TEXT -> {
                ic.commitText(finalText, 1)
            }
            AssistantSourceType.CURRENT_SENTENCE -> {
                val before = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
                val cursor = before.length
                val charsBefore = (cursor - session.sentenceStartOffset).coerceAtLeast(0)
                val charsAfter = (session.sentenceEndOffset - cursor).coerceAtLeast(0)

                if (charsBefore <= before.length && (charsBefore + charsAfter) > 0) {
                    ic.deleteSurroundingText(charsBefore, charsAfter)
                    ic.commitText(finalText, 1)
                } else {
                    // Safe fallback: match original sentence in textBefore
                    val original = session.originalText
                    val lastIdx = before.lastIndexOf(original, ignoreCase = true)
                    if (lastIdx != -1) {
                        val toDelete = before.length - lastIdx
                        ic.deleteSurroundingText(toDelete, 0)
                        ic.commitText(finalText, 1)
                    } else {
                        ic.commitText(finalText, 1)
                    }
                }
            }
            AssistantSourceType.MANUAL_INPUT -> {
                ic.commitText(finalText, 1)
            }
        }

        closeExpandedAssistant()
        return true
    }

    fun refreshAssistantText() {
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)?.toString()
        if (!selectedText.isNullOrBlank()) {
            _uiState.update { state ->
                val session = state.assistantSession ?: return@update state
                state.copy(
                    assistantSession = session.copy(
                        originalText = selectedText.trim(),
                        currentEditText = selectedText.trim(),
                        sourceType = AssistantSourceType.SELECTED_TEXT
                    )
                )
            }
            executeAssistantRewrite()
            return
        }

        val before = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(500, 0)?.toString() ?: ""
        val fullText = before + after
        val cursor = before.length

        if (fullText.isNotBlank()) {
            val span = sentenceAnalyzer.findCurrentSentence(fullText, cursor)
            val captured = span?.text?.trim() ?: fullText.trim()
            _uiState.update { state ->
                val session = state.assistantSession ?: return@update state
                state.copy(
                    assistantSession = session.copy(
                        originalText = captured,
                        currentEditText = captured,
                        sentenceStartOffset = span?.start ?: 0,
                        sentenceEndOffset = span?.end ?: fullText.length,
                        cursorOffset = cursor,
                        sourceType = AssistantSourceType.CURRENT_SENTENCE
                    )
                )
            }
            executeAssistantRewrite()
        }
    }
}
