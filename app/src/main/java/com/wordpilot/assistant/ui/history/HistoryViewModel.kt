package com.wordpilot.assistant.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.Document
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val documents: List<Document> = emptyList(),
    val historyEnabled: Boolean = false
)

class HistoryViewModel : ViewModel() {
    private val app = WordPilotApp.instance

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                app.historyRepository.getAllDocuments(),
                app.userPreferences.historyEnabled
            ) { docs, enabled ->
                HistoryUiState(documents = docs, historyEnabled = enabled)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteDocument(id: Int) {
        viewModelScope.launch {
            app.historyRepository.deleteDocument(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            app.historyRepository.deleteAll()
        }
    }
}
