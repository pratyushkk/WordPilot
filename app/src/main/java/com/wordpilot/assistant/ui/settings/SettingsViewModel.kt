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
    val selectedEngine: AIEngineType = AIEngineType.GECTOR,
    val isLocalModelInstalled: Boolean = false,
    val isGectorModelInstalled: Boolean = false,
    val blockCloudAi: Boolean = false
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
            app.modelRepository.modelState,
            prefs.blockCloudAi
        ) { engineId, _, blockCloud ->
            val engine = AIEngineType.fromId(engineId)
            val effective = if (blockCloud && engine == AIEngineType.GEMINI_FLASH) AIEngineType.GECTOR else engine
            Triple(
                effective,
                app.modelRepository.isModelInstalled(),
                blockCloud
            )
        }

        viewModelScope.launch {
            combine(
                prefs.theme,
                checkSettingsFlow,
                extraSettingsFlow,
                engineFlow
            ) { theme, (grammar, spell, auto), (history, autoPause, apiKey), (selectedEngine, isModelInstalled, blockCloud) ->
                SettingsUiState(
                    theme = theme,
                    grammarCheckEnabled = grammar,
                    spellCheckEnabled = spell,
                    autoCheckEnabled = auto,
                    historyEnabled = history,
                    autoPauseBankingEnabled = autoPause,
                    geminiApiKey = apiKey,
                    selectedEngine = selectedEngine,
                    isLocalModelInstalled = isModelInstalled,
                    isGectorModelInstalled = app.modelRepository.isGectorModelInstalled(),
                    blockCloudAi = blockCloud
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setBlockCloudAi(blocked: Boolean) {
        viewModelScope.launch {
            prefs.setBlockCloudAi(blocked)
            app.rewriteCoordinator.setBlockCloudAi(blocked)
            if (blocked) {
                setSelectedEngine(AIEngineType.GECTOR)
            }
        }
    }

    fun setSelectedEngine(engine: AIEngineType) {
        viewModelScope.launch {
            val targetEngine = if (_uiState.value.blockCloudAi && engine == AIEngineType.GEMINI_FLASH) {
                AIEngineType.GECTOR
            } else {
                engine
            }
            prefs.setSelectedEngine(targetEngine.id)
            app.rewriteCoordinator.setEngineType(targetEngine)
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
