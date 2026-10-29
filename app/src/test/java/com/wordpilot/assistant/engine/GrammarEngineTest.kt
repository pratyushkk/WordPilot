package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.engine.impl.RuleBasedGrammarEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GrammarEngineTest {

    private lateinit var grammarEngine: RuleBasedGrammarEngine

    @Before
    fun setUp() {
        grammarEngine = RuleBasedGrammarEngine()
    }

    @Test
    fun `test repeated words detection`() = runBlocking {
        val text = "I went went to the market."
        val corrections = grammarEngine.check(text)

        val repeatedWordCorrection = corrections.find { it.ruleId == "REPEATED_WORD" }
        assertNotNull("Should detect repeated word 'went'", repeatedWordCorrection)
        assertEquals("went", repeatedWordCorrection?.suggestedReplacement)
    }

    @Test
    fun `test subject verb agreement - she go`() = runBlocking {
        val text = "She go to school every day."
        val corrections = grammarEngine.check(text)

        val svCorrection = corrections.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should detect 'She go' subject-verb error", svCorrection)
        assertEquals("goes", svCorrection?.suggestedReplacement)
    }

    @Test
    fun `test subject verb agreement - I has`() = runBlocking {
        val text = "I has a new phone."
        val corrections = grammarEngine.check(text)

        val svCorrection = corrections.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should detect 'I has' subject-verb error", svCorrection)
        assertEquals("have", svCorrection?.suggestedReplacement)
    }

    @Test
    fun `test article a vs an - a apple`() = runBlocking {
        val text = "This is a apple."
        val corrections = grammarEngine.check(text)

        val articleCorrection = corrections.find { it.ruleId == "ARTICLE_A_AN" }
        assertNotNull("Should detect 'a apple' error", articleCorrection)
        assertEquals("an", articleCorrection?.suggestedReplacement)
    }

    @Test
    fun `test punctuation spacing - missing space after comma`() = runBlocking {
        val text = "hello,how are you?"
        val corrections = grammarEngine.check(text)

        val punctCorrection = corrections.find { it.ruleId.startsWith("PUNCTUATION_SPACING") }
        assertNotNull("Should detect missing space after comma", punctCorrection)
        assertEquals(", h", punctCorrection?.suggestedReplacement)
    }

    @Test
    fun `test confused words - their going`() = runBlocking {
        val text = "Their going to the store."
        val corrections = grammarEngine.check(text)

        val confusedCorrection = corrections.find { it.ruleId.startsWith("CONFUSED_WORDS") }
        assertNotNull("Should detect 'Their going' error", confusedCorrection)
        assertEquals("They're", confusedCorrection?.suggestedReplacement)
    }

    @Test
    fun `test capitalization at sentence start`() = runBlocking {
        val text = "this is a test sentence."
        val corrections = grammarEngine.check(text)

        val capCorrection = corrections.find { it.category == CorrectionCategory.CAPITALIZATION }
        assertNotNull("Should detect lowercase sentence start", capCorrection)
        assertEquals("T", capCorrection?.suggestedReplacement)
    }

    @Test
    fun `test pronoun I capitalization`() = runBlocking {
        val text = "Yesterday i walked home."
        val corrections = grammarEngine.check(text)

        val iCorrection = corrections.find { it.ruleId == "CAPITALIZATION.PRONOUN_I" }
        assertNotNull("Should detect lowercase 'i'", iCorrection)
        assertEquals("I", iCorrection?.suggestedReplacement)
    }
}
