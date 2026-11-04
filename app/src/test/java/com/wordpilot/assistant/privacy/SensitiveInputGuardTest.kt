package com.wordpilot.assistant.privacy

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test

class SensitiveInputGuardTest {

    @Test
    fun `test null EditorInfo is treated as sensitive for safety`() {
        assertTrue("Null EditorInfo should be considered sensitive", SensitiveInputGuard.isSensitiveField(null))
        assertFalse("Null EditorInfo should not be analyzed", SensitiveInputGuard.shouldAnalyzeField(null))
    }

    @Test
    fun `test password fields are recognized as sensitive`() {
        val passwordInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue("Password field should be sensitive", SensitiveInputGuard.isSensitiveField(passwordInfo))
        assertFalse("Password field should not be analyzed", SensitiveInputGuard.shouldAnalyzeField(passwordInfo))
    }

    @Test
    fun `test PIN numeric password fields are recognized as sensitive`() {
        val pinInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        assertTrue("PIN field should be sensitive", SensitiveInputGuard.isSensitiveField(pinInfo))
        assertFalse("PIN field should not be analyzed", SensitiveInputGuard.shouldAnalyzeField(pinInfo))
    }

    @Test
    fun `test regular text field is safe to analyze`() {
        val normalText = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        }
        assertFalse("Regular text should not be sensitive", SensitiveInputGuard.isSensitiveField(normalText))
        assertTrue("Regular text should be analyzed", SensitiveInputGuard.shouldAnalyzeField(normalText))
    }

    @Test
    fun `test no suggestions flag disables analysis`() {
        val noSuggestions = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        assertTrue("No suggestions field should be treated as sensitive", SensitiveInputGuard.isSensitiveField(noSuggestions))
        assertFalse("No suggestions field should not be analyzed", SensitiveInputGuard.shouldAnalyzeField(noSuggestions))
    }
}
