package com.wordpilot.assistant.domain.models

sealed interface TextOperationResult {
    data object Success : TextOperationResult
    data class Failure(val reason: String) : TextOperationResult
    data object StaleSnapshot : TextOperationResult
    data object UnsupportedEditor : TextOperationResult
    data object EditorModified : TextOperationResult
}
