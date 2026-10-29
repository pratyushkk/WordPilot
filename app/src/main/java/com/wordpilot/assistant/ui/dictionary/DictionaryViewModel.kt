package com.wordpilot.assistant.ui.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.data.database.IgnoredRuleEntity
import com.wordpilot.assistant.domain.models.DictionaryEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DictionaryUiState(
    val words: List<DictionaryEntry> = emptyList(),
    val ignoredRules: List<IgnoredRuleEntity> = emptyList()
)

class DictionaryViewModel : ViewModel() {
    private val app = WordPilotApp.instance
    private val repo = app.dictionaryRepository

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.getAllEntries(),
                repo.getAllIgnoredRules()
            ) { words, rules ->
                DictionaryUiState(words = words, ignoredRules = rules)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addWord(word: String) {
        viewModelScope.launch {
            repo.addWord(word.trim())
            app.spellEngine.addWord(word.trim().lowercase())
        }
    }

    fun removeWord(word: String) {
        viewModelScope.launch {
            repo.removeWord(word)
            app.spellEngine.removeWord(word.lowercase())
        }
    }

    fun restoreRule(rule: IgnoredRuleEntity) {
        viewModelScope.launch {
            repo.restoreRule(rule)
        }
    }
}
