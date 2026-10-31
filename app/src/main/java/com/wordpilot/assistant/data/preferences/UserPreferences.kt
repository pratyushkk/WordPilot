package com.wordpilot.assistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wordpilot.assistant.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wordpilot_prefs")

class UserPreferences(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager()
) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val GRAMMAR_CHECK_ENABLED = booleanPreferencesKey("grammar_check_enabled")
        val SPELL_CHECK_ENABLED = booleanPreferencesKey("spell_check_enabled")
        val AUTO_CHECK_ENABLED = booleanPreferencesKey("auto_check_enabled")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val SELECTED_ENGINE = stringPreferencesKey("selected_engine")
        val AUTO_PAUSE_BANKING_ENABLED = booleanPreferencesKey("auto_pause_banking_enabled")
    }

    val autoPauseBankingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_PAUSE_BANKING_ENABLED] ?: false
    }

    val geminiApiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        val raw = prefs[GEMINI_API_KEY] ?: return@map null
        cryptoManager.decrypt(raw)
    }

    val selectedEngine: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_ENGINE] ?: "gemini"
    }

    val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_KEY] ?: "system"
    }

    val grammarCheckEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[GRAMMAR_CHECK_ENABLED] ?: true
    }

    val spellCheckEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SPELL_CHECK_ENABLED] ?: true
    }

    val autoCheckEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_CHECK_ENABLED] ?: true
    }

    val historyEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HISTORY_ENABLED] ?: false
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme
        }
    }

    suspend fun setGrammarCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[GRAMMAR_CHECK_ENABLED] = enabled
        }
    }

    suspend fun setSpellCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SPELL_CHECK_ENABLED] = enabled
        }
    }

    suspend fun setAutoCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_CHECK_ENABLED] = enabled
        }
    }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HISTORY_ENABLED] = enabled
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        val encrypted = if (apiKey.isNotBlank()) cryptoManager.encrypt(apiKey.trim()) else ""
        context.dataStore.edit { prefs ->
            prefs[GEMINI_API_KEY] = encrypted
        }
    }

    suspend fun setSelectedEngine(engine: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_ENGINE] = engine
        }
    }

    suspend fun setAutoPauseBankingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_PAUSE_BANKING_ENABLED] = enabled
        }
    }

    suspend fun clearAll() {
        cryptoManager.clearKey()
        context.dataStore.edit { it.clear() }
    }
}
