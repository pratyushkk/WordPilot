package com.wordpilot.assistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wordpilot_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val GRAMMAR_CHECK_ENABLED = booleanPreferencesKey("grammar_check_enabled")
        val SPELL_CHECK_ENABLED = booleanPreferencesKey("spell_check_enabled")
        val AUTO_CHECK_ENABLED = booleanPreferencesKey("auto_check_enabled")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val SELECTED_ENGINE = stringPreferencesKey("selected_engine")
        // Gboard Keyboard Customization Keys
        val KEYBOARD_HEIGHT_DP = intPreferencesKey("keyboard_height_dp")
        val KEYBOARD_BOTTOM_PADDING_DP = intPreferencesKey("keyboard_bottom_padding_dp")
        val KEYBOARD_SIDE_PADDING_DP = intPreferencesKey("keyboard_side_padding_dp")
        val KEYBOARD_SHOW_NUMBER_ROW = booleanPreferencesKey("keyboard_show_number_row")
        val KEYBOARD_SHOW_KEY_HINTS = booleanPreferencesKey("keyboard_show_key_hints")
        val KEYBOARD_HAPTIC_ENABLED = booleanPreferencesKey("keyboard_haptic_enabled")
        val AUTO_PAUSE_BANKING_ENABLED = booleanPreferencesKey("auto_pause_banking_enabled")
    }

    val autoPauseBankingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_PAUSE_BANKING_ENABLED] ?: false
    }

    val geminiApiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[GEMINI_API_KEY]
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

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    val keyboardHeightDp: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEYBOARD_HEIGHT_DP] ?: 46
    }

    val keyboardBottomPaddingDp: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEYBOARD_BOTTOM_PADDING_DP] ?: 0
    }

    val keyboardSidePaddingDp: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEYBOARD_SIDE_PADDING_DP] ?: 0
    }

    val keyboardShowNumberRow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEYBOARD_SHOW_NUMBER_ROW] ?: true
    }

    val keyboardShowKeyHints: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEYBOARD_SHOW_KEY_HINTS] ?: true
    }

    val keyboardHapticEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEYBOARD_HAPTIC_ENABLED] ?: true
    }

    suspend fun setKeyboardHeightDp(height: Int) {
        context.dataStore.edit { prefs -> prefs[KEYBOARD_HEIGHT_DP] = height }
    }

    suspend fun setKeyboardBottomPaddingDp(padding: Int) {
        context.dataStore.edit { prefs -> prefs[KEYBOARD_BOTTOM_PADDING_DP] = padding }
    }

    suspend fun setKeyboardSidePaddingDp(padding: Int) {
        context.dataStore.edit { prefs -> prefs[KEYBOARD_SIDE_PADDING_DP] = padding }
    }

    suspend fun setKeyboardShowNumberRow(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEYBOARD_SHOW_NUMBER_ROW] = enabled }
    }

    suspend fun setKeyboardShowKeyHints(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEYBOARD_SHOW_KEY_HINTS] = enabled }
    }

    suspend fun setKeyboardHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEYBOARD_HAPTIC_ENABLED] = enabled }
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

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[GEMINI_API_KEY] = apiKey
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
        context.dataStore.edit { it.clear() }
    }
}
