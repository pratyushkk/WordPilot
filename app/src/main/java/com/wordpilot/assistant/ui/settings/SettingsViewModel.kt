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
    val selectedEngine: AIEngineType = AIEngineType.GEMINI_FLASH,
    val isLocalModelInstalled: Boolean = false,
    val blockCloudAi: Boolean = false,
    val saplingApiKey: String? = null,
    val saplingEnabled: Boolean = true,
    val languageToolEnabled: Boolean = true,
    val languageToolBaseUrl: String = "https://api.languagetool.org",
    val grammarProviderPriority: String = "sapling,languagetool"
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
                prefs.historyEnabled
            ) { theme, grammar, spell, auto, history ->
                SettingsUiState(
                    theme = theme,
                    grammarCheckEnabled = grammar,
                    spellCheckEnabled = spell,
                    autoCheckEnabled = auto,
                    historyEnabled = history
                )
            }.combine(prefs.autoPauseBankingEnabled) { state, autoPause ->
                state.copy(autoPauseBankingEnabled = autoPause)
            }.combine(prefs.geminiApiKey) { state, geminiKey ->
                state.copy(geminiApiKey = geminiKey)
            }.combine(prefs.selectedEngine) { state, engineId ->
                state.copy(
                    selectedEngine = AIEngineType.fromId(engineId),
                    isLocalModelInstalled = app.modelRepository.isModelInstalled()
                )
            }.combine(prefs.blockCloudAi) { state, blocked ->
                state.copy(blockCloudAi = blocked)
            }.combine(prefs.saplingApiKey) { state, saplingKey ->
                state.copy(saplingApiKey = saplingKey)
            }.combine(prefs.saplingEnabled) { state, saplingEnabled ->
                state.copy(saplingEnabled = saplingEnabled)
            }.combine(prefs.languageToolEnabled) { state, ltEnabled ->
                state.copy(languageToolEnabled = ltEnabled)
            }.combine(prefs.languageToolBaseUrl) { state, ltUrl ->
                state.copy(languageToolBaseUrl = ltUrl)
            }.combine(prefs.grammarProviderPriority) { state, priority ->
                state.copy(grammarProviderPriority = priority)
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
                setSelectedEngine(AIEngineType.RULE_BASED)
            }
        }
    }

    fun setSelectedEngine(engine: AIEngineType = AIEngineType.GEMINI_FLASH) {
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

    fun setSaplingApiKey(key: String) {
        viewModelScope.launch { prefs.setSaplingApiKey(key) }
    }

    fun setSaplingEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setSaplingEnabled(enabled) }
    }

    fun setLanguageToolBaseUrl(url: String) {
        viewModelScope.launch { prefs.setLanguageToolBaseUrl(url) }
    }

    fun setLanguageToolEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setLanguageToolEnabled(enabled) }
    }

    fun setGrammarProviderPriority(priority: String) {
        viewModelScope.launch { prefs.setGrammarProviderPriority(priority) }
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
