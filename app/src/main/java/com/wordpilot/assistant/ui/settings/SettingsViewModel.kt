package com.wordpilot.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: String = "system",
    val grammarCheckEnabled: Boolean = true,
    val spellCheckEnabled: Boolean = true,
    val autoCheckEnabled: Boolean = true,
    val historyEnabled: Boolean = false,
    val autoPauseBankingEnabled: Boolean = false,
    val geminiApiKey: String? = null
)

class SettingsViewModel : ViewModel() {
    private val app = WordPilotApp.instance
    private val prefs = app.userPreferences

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.theme,
                prefs.grammarCheckEnabled,
                prefs.spellCheckEnabled,
                prefs.autoCheckEnabled,
                combine(prefs.historyEnabled, prefs.autoPauseBankingEnabled, prefs.geminiApiKey) { h, b, k -> Triple(h, b, k) }
            ) { theme, grammar, spell, auto, (history, autoPause, apiKey) ->
                SettingsUiState(
                    theme = theme,
                    grammarCheckEnabled = grammar,
                    spellCheckEnabled = spell,
                    autoCheckEnabled = auto,
                    historyEnabled = history,
                    autoPauseBankingEnabled = autoPause,
                    geminiApiKey = apiKey
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            app.updateGeminiApiKey(key)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun setGrammarCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setGrammarCheckEnabled(enabled) }
    }

    fun setSpellCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setSpellCheckEnabled(enabled) }
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoCheckEnabled(enabled) }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setHistoryEnabled(enabled) }
    }

    fun setAutoPauseBankingEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoPauseBankingEnabled(enabled) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            prefs.clearAll()
            app.dictionaryRepository.deleteAll()
            app.dictionaryRepository.clearAllIgnoredRules()
            app.historyRepository.deleteAll()
        }
    }
}
