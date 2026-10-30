package com.wordpilot.assistant.engine.impl

import android.content.Context
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.engine.SpellEngine
import com.wordpilot.assistant.engine.offline.OfflineGrammarEngineFacade

class TrieSpellEngine(
    private val context: Context? = null,
    val facade: OfflineGrammarEngineFacade = OfflineGrammarEngineFacade(context)
) : SpellEngine {

    override fun isAvailable(): Boolean = true

    override fun addWord(word: String) {
        facade.addCustomWord(word)
    }

    override fun removeWord(word: String) {
        facade.removeCustomWord(word)
    }

    override fun isKnownWord(word: String): Boolean {
        return facade.isKnownWord(word)
    }

    override fun getSuggestions(word: String, maxResults: Int): List<String> {
        return facade.getSpellingSuggestions(word, maxResults)
    }

    override suspend fun check(text: String): List<Correction> {
        if (text.isBlank()) return emptyList()
        val all = facade.checkText(text)
        return all.filter { it.category == CorrectionCategory.SPELLING }
    }
}
