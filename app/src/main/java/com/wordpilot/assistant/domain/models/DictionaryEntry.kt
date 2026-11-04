package com.wordpilot.assistant.domain.models

data class DictionaryEntry(
    val id: Int = 0,
    val word: String,
    val category: String = "custom",
    val addedTimestamp: Long = System.currentTimeMillis()
)
