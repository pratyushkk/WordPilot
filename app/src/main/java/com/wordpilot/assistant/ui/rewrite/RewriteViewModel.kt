package com.wordpilot.assistant.ui.rewrite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.RewriteResult
import com.wordpilot.assistant.domain.models.RewriteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RewriteUiState(
    val inputText: String = "",
    val selectedStyle: RewriteStyle = RewriteStyle.IMPROVE_CLARITY,
    val customInstruction: String = "",
    val isProcessing: Boolean = false,
    val result: RewriteResult? = null,
    val selectedEngine: AIEngineType = AIEngineType.GEMINI_FLASH,
    val isLocalModelInstalled: Boolean = false,
    val isGeminiConfigured: Boolean = false
)

class RewriteViewModel : ViewModel() {
    private val app = WordPilotApp.instance
    private val rewriteUseCase = app.rewriteUseCase

    private val _uiState = MutableStateFlow(RewriteUiState())
    val uiState: StateFlow<RewriteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                app.rewriteCoordinator.selectedEngine,
                app.modelRepository.modelState,
                app.userPreferences.geminiApiKey
            ) { engine, _, apiKey ->
                Triple(engine, app.modelRepository.isModelInstalled(), !apiKey.isNullOrBlank())
            }.collect { (engine, isInstalled, hasKey) ->
                _uiState.update {
                    it.copy(
                        selectedEngine = engine,
                        isLocalModelInstalled = isInstalled,
                        isGeminiConfigured = hasKey
                    )
                }
            }
        }
    }

    fun setSelectedEngine(engine: AIEngineType) {
        viewModelScope.launch {
            app.userPreferences.setSelectedEngine(engine.id)
            app.rewriteCoordinator.setEngineType(engine)
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectStyle(style: RewriteStyle) {
        _uiState.update { it.copy(selectedStyle = style) }
    }

    fun updateCustomInstruction(instruction: String) {
        _uiState.update { it.copy(customInstruction = instruction) }
    }

    fun rewrite() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, result = null) }

            val state = _uiState.value
            val result = rewriteUseCase.rewrite(
                text = state.inputText,
                style = state.selectedStyle,
                customInstruction = if (state.selectedStyle == RewriteStyle.CUSTOM)
                    state.customInstruction else null,
                forcedEngine = state.selectedEngine
            )

            _uiState.update { it.copy(isProcessing = false, result = result) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        rewriteUseCase.cancel()
    }
}
