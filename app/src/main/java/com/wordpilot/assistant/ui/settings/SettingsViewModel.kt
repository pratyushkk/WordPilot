package com.wordpilot.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.AIEngineType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: String = "system",
    val grammarCheckEnabled: Boolean = true,
    val spellCheckEnabled: Boolean = true,
    val autoCheckEnabled: Boolean = true,
    val historyEnabled: Boolean = false,
    val autoPauseBankingEnabled: Boolean = false,
    val geminiApiKey: String? = null,
    val selectedEngine: AIEngineType = AIEngineType.LOCAL_AI,
    val isLocalModelInstalled: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val app = WordPilotApp.instance
    private val prefs = app.userPreferences

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val checkSettingsFlow = combine(
            prefs.grammarCheckEnabled,
            prefs.spellCheckEnabled,
            prefs.autoCheckEnabled
        ) { grammar, spell, auto ->
            Triple(grammar, spell, auto)
        }

        val extraSettingsFlow = combine(
            prefs.historyEnabled,
            prefs.autoPauseBankingEnabled,
            prefs.geminiApiKey
        ) { history, autoPause, apiKey ->
            Triple(history, autoPause, apiKey)
        }

        val engineFlow = combine(
            prefs.selectedEngine,
            app.modelRepository.modelState
        ) { engineId, _ ->
            Pair(AIEngineType.fromId(engineId), app.modelRepository.isModelInstalled())
        }

        viewModelScope.launch {
            combine(
                prefs.theme,
                checkSettingsFlow,
                extraSettingsFlow,
                engineFlow
            ) { theme, (grammar, spell, auto), (history, autoPause, apiKey), (selectedEngine, isModelInstalled) ->
                SettingsUiState(
                    theme = theme,
                    grammarCheckEnabled = grammar,
                    spellCheckEnabled = spell,
                    autoCheckEnabled = auto,
                    historyEnabled = history,
                    autoPauseBankingEnabled = autoPause,
                    geminiApiKey = apiKey,
                    selectedEngine = selectedEngine,
                    isLocalModelInstalled = isModelInstalled
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setSelectedEngine(engine: AIEngineType) {
        viewModelScope.launch {
            prefs.setSelectedEngine(engine.id)
            app.rewriteCoordinator.setEngineType(engine)
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
