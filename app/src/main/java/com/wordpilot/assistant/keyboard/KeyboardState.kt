package com.wordpilot.assistant.keyboard

import com.wordpilot.assistant.domain.models.Correction

enum class KeyboardMode {
    QWERTY,
    NUMBERS,
    SYMBOLS
}

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
    val isAnalyzing: Boolean = false
)
