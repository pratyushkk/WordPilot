package com.wordpilot.assistant.keyboard

import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.RewriteStyle

enum class KeyboardMode {
    QWERTY,
    NUMBERS,
    SYMBOLS,
    EXPANDED_ASSISTANT
}

enum class AssistantSourceType(val displayName: String) {
    CURRENT_SENTENCE("Current sentence"),
    SELECTED_TEXT("Selected text"),
    MANUAL_INPUT("Custom input")
}

data class AssistantSessionState(
    val originalText: String = "",
    val sourceType: AssistantSourceType = AssistantSourceType.CURRENT_SENTENCE,
    val sentenceStartOffset: Int = 0,
    val sentenceEndOffset: Int = 0,
    val cursorOffset: Int = 0,
    val currentEditText: String = "",
    val rewrittenText: String = "",
    val selectedStyle: RewriteStyle = RewriteStyle.FIX_GRAMMAR,
    val customInstruction: String = "",
    val selectedEngine: AIEngineType = AIEngineType.GEMINI_FLASH,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val lastSuccess: Boolean = false
)

enum class ShiftState {
    OFF,
    SHIFTED,
    CAPS_LOCK;

    fun next(): ShiftState = when (this) {
        OFF -> SHIFTED
        SHIFTED -> CAPS_LOCK
        CAPS_LOCK -> OFF
    }
}

data class KeyboardUiState(
    val mode: KeyboardMode = KeyboardMode.QWERTY,
    val shiftState: ShiftState = ShiftState.OFF,
    val wordSuggestions: List<String> = emptyList(),
    val activeCorrection: Correction? = null,
    val isCorrectionCardExpanded: Boolean = false,
    val isSensitiveField: Boolean = false,
    val isAnalyzing: Boolean = false,
    val assistantSession: AssistantSessionState? = null
)
