package com.wordpilot.assistant.privacy

import android.text.InputType
import android.view.inputmethod.EditorInfo

object SensitiveInputGuard {
    
    fun isSensitiveField(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return true // Be conservative
        
        val inputType = editorInfo.inputType
        val typeClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        
        return when {
            // Password fields
            typeClass == InputType.TYPE_CLASS_TEXT &&
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD -> true
            typeClass == InputType.TYPE_CLASS_TEXT &&
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> true
            typeClass == InputType.TYPE_CLASS_TEXT &&
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> true
            // Number password (PIN)
            typeClass == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD -> true
            // Flag-based detection
            inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0 -> true
            // OTP detection via private IME options
            editorInfo.privateImeOptions?.contains("otp", ignoreCase = true) == true -> true
            editorInfo.privateImeOptions?.contains("password", ignoreCase = true) == true -> true
            // Number-only fields are likely PINs or OTPs in certain contexts
            typeClass == InputType.TYPE_CLASS_NUMBER && 
                editorInfo.privateImeOptions?.contains("pin", ignoreCase = true) == true -> true
            else -> false
        }
    }

    fun isTextInputField(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val typeClass = editorInfo.inputType and InputType.TYPE_MASK_CLASS
        return typeClass == InputType.TYPE_CLASS_TEXT
    }

    fun shouldAnalyzeField(editorInfo: EditorInfo?): Boolean {
        return isTextInputField(editorInfo) && !isSensitiveField(editorInfo)
    }
}
