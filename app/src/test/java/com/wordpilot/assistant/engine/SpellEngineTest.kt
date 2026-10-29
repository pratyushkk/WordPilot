package com.wordpilot.assistant.engine

import com.wordpilot.assistant.engine.impl.TrieSpellEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SpellEngineTest {

    private lateinit var spellEngine: TrieSpellEngine

    @Before
    fun setUp() {
        spellEngine = TrieSpellEngine(null) // Uses built-in core words
    }

    @Test
    fun `test known core words return true`() {
        assertTrue("Common word 'the' should be known", spellEngine.isKnownWord("the"))
        assertTrue("Common word 'assistant' should be known", spellEngine.isKnownWord("assistant"))
        assertTrue("Case insensitive check for 'Hello'", spellEngine.isKnownWord("Hello"))
    }

    @Test
    fun `test unknown words return false`() {
        assertFalse("Misspelled word should be unknown", spellEngine.isKnownWord("asistantt"))
        assertFalse("Misspelled word should be unknown", spellEngine.isKnownWord("writting"))
    }

    @Test
    fun `test adding word to personal dictionary`() {
        val customTerm = "MotorolaEdge"
        assertFalse(spellEngine.isKnownWord(customTerm))

        spellEngine.addWord(customTerm)
        assertTrue("Added word should now be known", spellEngine.isKnownWord(customTerm))

        spellEngine.removeWord(customTerm)
        assertFalse("Removed word should no longer be known", spellEngine.isKnownWord(customTerm))
    }

    @Test
    fun `test spelling suggestions generated for typo`() {
        // "thnk" -> "think"
        val suggestions = spellEngine.getSuggestions("thnk", 3)
        assertTrue("Should suggest 'think' for 'thnk'", suggestions.contains("think"))
    }

    @Test
    fun `test check method finds misspelled words and returns corrections`() = runBlocking {
        val text = "This is a writting test"
        val corrections = spellEngine.check(text)

        val spellingError = corrections.find { it.originalText == "writting" }
        assertNotNull("Should detect misspelled word 'writting'", spellingError)
    }
}
