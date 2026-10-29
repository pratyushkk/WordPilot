package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.Correction

interface SpellEngine {
    suspend fun check(text: String): List<Correction>
    fun isAvailable(): Boolean
    fun addWord(word: String)
    fun removeWord(word: String)
    fun isKnownWord(word: String): Boolean
    fun getSuggestions(word: String, maxResults: Int = 5): List<String>
}
