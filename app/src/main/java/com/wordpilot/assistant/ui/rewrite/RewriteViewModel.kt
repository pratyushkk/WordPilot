package com.wordpilot.assistant.ui.rewrite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.RewriteResult
import com.wordpilot.assistant.domain.models.RewriteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RewriteUiState(
    val inputText: String = "",
    val selectedStyle: RewriteStyle = RewriteStyle.IMPROVE_CLARITY,
    val customInstruction: String = "",
    val isProcessing: Boolean = false,
    val result: RewriteResult? = null
)

class RewriteViewModel : ViewModel() {
    private val app = WordPilotApp.instance
    private val rewriteUseCase = app.rewriteUseCase

    private val _uiState = MutableStateFlow(RewriteUiState())
    val uiState: StateFlow<RewriteUiState> = _uiState.asStateFlow()

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
                    state.customInstruction else null
            )
            
            _uiState.update { it.copy(isProcessing = false, result = result) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        rewriteUseCase.cancel()
    }
}
