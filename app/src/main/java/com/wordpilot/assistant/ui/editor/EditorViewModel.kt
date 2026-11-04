package com.wordpilot.assistant.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.Document
import com.wordpilot.assistant.utils.Debouncer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EditorUiState(
    val text: String = "",
    val documentVersion: Long = 0L,
    val corrections: List<Correction> = emptyList(),
    val isChecking: Boolean = false,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList(),
    val documentId: Int? = null,
    val documentTitle: String = "Untitled"
)

class EditorViewModel : ViewModel() {
    private val app = WordPilotApp.instance
    private val correctionUseCase = app.correctionUseCase
    private val historyRepository = app.historyRepository
    private val dictionaryRepository = app.dictionaryRepository

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val debouncer = Debouncer(viewModelScope, 500L)
    private var autoCheckEnabled = true

    init {
        viewModelScope.launch {
            app.userPreferences.autoCheckEnabled.collect { enabled ->
                autoCheckEnabled = enabled
            }
        }
    }

    fun loadDocument(documentId: Int) {
        viewModelScope.launch {
            val doc = historyRepository.getDocument(documentId) ?: return@launch
            _uiState.update { it.copy(
                text = doc.content,
                documentId = doc.id,
                documentTitle = doc.title,
                wordCount = com.wordpilot.assistant.utils.TextUtils.wordCount(doc.content),
                charCount = doc.content.length
            )}
        }
    }

    fun onTextChanged(newText: String) {
        val currentText = _uiState.value.text
        if (currentText != newText) {
            // Push to undo stack
            _uiState.update { it.copy(
                text = newText,
                documentVersion = it.documentVersion + 1,
                wordCount = com.wordpilot.assistant.utils.TextUtils.wordCount(newText),
                charCount = newText.length,
                undoStack = (it.undoStack + currentText).takeLast(50),
                redoStack = emptyList()
            )}

            if (autoCheckEnabled) {
                debouncer.debounce {
                    checkText()
                }
            }
        }
    }

    fun checkText() {
        viewModelScope.launch {
            val text = _uiState.value.text
            if (text.isBlank()) {
                _uiState.update { it.copy(corrections = emptyList(), isChecking = false) }
                return@launch
            }

            _uiState.update { it.copy(isChecking = true) }
            
            try {
                val corrections = correctionUseCase.checkFullText(text)
                // Filter out ignored rules and dictionary words
                val filtered = filterCorrections(corrections)
                _uiState.update { it.copy(corrections = filtered, isChecking = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isChecking = false) }
            }
        }
    }

    fun acceptCorrection(correction: Correction) {
        val state = _uiState.value
        val text = state.text
        if (!correctionUseCase.validateCorrection(text, correction)) {
            // Text has changed or span is stale: re-run check and safely abort
            checkText()
            return
        }

        val result = correctionUseCase.applyCorrection(text, correction) ?: run {
            checkText()
            return
        }

        val remaining = correctionUseCase.adjustRemainingCorrections(
            state.corrections, correction
        )

        _uiState.update { it.copy(
            text = result,
            documentVersion = it.documentVersion + 1,
            corrections = remaining,
            wordCount = com.wordpilot.assistant.utils.TextUtils.wordCount(result),
            charCount = result.length,
            undoStack = (it.undoStack + text).takeLast(50),
            redoStack = emptyList()
        )}
    }

    fun dismissCorrection(correction: Correction) {
        _uiState.update { it.copy(
            corrections = it.corrections.filter { c -> c.id != correction.id }
        )}
    }

    fun addToDictionary(correction: Correction) {
        viewModelScope.launch {
            val word = correction.originalText.lowercase().trim()
            dictionaryRepository.addWord(word)
            app.spellEngine.addWord(word)
            dismissCorrection(correction)
        }
    }

    fun ignoreRule(correction: Correction) {
        viewModelScope.launch {
            dictionaryRepository.ignoreRule(correction.ruleId, correction.originalText)
            dismissCorrection(correction)
        }
    }

    fun undo() {
        val stack = _uiState.value.undoStack
        if (stack.isEmpty()) return
        val previous = stack.last()
        _uiState.update { it.copy(
            text = previous,
            undoStack = stack.dropLast(1),
            redoStack = (it.redoStack + it.text).takeLast(50),
            wordCount = com.wordpilot.assistant.utils.TextUtils.wordCount(previous),
            charCount = previous.length,
            corrections = emptyList()
        )}
    }

    fun redo() {
        val stack = _uiState.value.redoStack
        if (stack.isEmpty()) return
        val next = stack.last()
        _uiState.update { it.copy(
            text = next,
            redoStack = stack.dropLast(1),
            undoStack = (it.undoStack + it.text).takeLast(50),
            wordCount = com.wordpilot.assistant.utils.TextUtils.wordCount(next),
            charCount = next.length,
            corrections = emptyList()
        )}
    }

    fun clearText() {
        _uiState.update { it.copy(
            text = "",
            corrections = emptyList(),
            wordCount = 0,
            charCount = 0,
            undoStack = (it.undoStack + it.text).takeLast(50),
            redoStack = emptyList()
        )}
    }

    fun saveDocument(title: String? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val docTitle = title ?: state.documentTitle
            if (state.text.isBlank()) return@launch

            val doc = Document(
                id = state.documentId ?: 0,
                title = docTitle,
                content = state.text
            )

            if (state.documentId != null) {
                historyRepository.updateDocument(doc)
            } else {
                val id = historyRepository.saveDocument(doc)
                _uiState.update { it.copy(documentId = id.toInt(), documentTitle = docTitle) }
            }
        }
    }

    private suspend fun filterCorrections(corrections: List<Correction>): List<Correction> {
        return corrections.filter { correction ->
            val isIgnored = dictionaryRepository.isRuleIgnored(correction.ruleId, correction.originalText)
            val isInDictionary = if (correction.category == com.wordpilot.assistant.domain.models.CorrectionCategory.SPELLING) {
                dictionaryRepository.wordExists(correction.originalText.lowercase())
            } else false
            !isIgnored && !isInDictionary
        }
    }

    override fun onCleared() {
        super.onCleared()
        debouncer.cancel()
    }
}
