package com.wordpilot.assistant

import com.wordpilot.assistant.engine.impl.RuleBasedGrammarEngine
import com.wordpilot.assistant.engine.impl.TrieSpellEngine
import com.wordpilot.assistant.engine.CorrectionCoordinator
import com.wordpilot.assistant.engine.gector.GectorOnnxEngine
import com.wordpilot.assistant.engine.offline.OfflineGrammarEngineFacade
import com.wordpilot.assistant.engine.offline.model.AnalysisOptions
import com.wordpilot.assistant.security.CryptoManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ProductionReleaseValidationTest {

    @Test
    fun testKeyboardClassesAreCompletelyRemoved() {
        val deletedClasses = listOf(
            "com.wordpilot.assistant.keyboard.WordPilotInputMethodService",
            "com.wordpilot.assistant.keyboard.WordPilotKeyboardView",
            "com.wordpilot.assistant.keyboard.KeyboardSuggestionController",
            "com.wordpilot.assistant.keyboard.KeyLayout",
            "com.wordpilot.assistant.keyboard.KeyboardState",
            "com.wordpilot.assistant.ui.onboarding.OnboardingScreenKt",
            "com.wordpilot.assistant.utils.KeyboardVisibilityTracker"
        )

        for (className in deletedClasses) {
            try {
                Class.forName(className)
                fail("Class $className should be completely deleted from the codebase!")
            } catch (expected: ClassNotFoundException) {
                // Expected: class does not exist
            }
        }
    }

    @Test
    fun testSinglePassProofreadingEngineAccuracy() = runBlocking {
        val facade = OfflineGrammarEngineFacade(null)

        val text = "She go to school every day. I recieved your email."
        val corrections = facade.analyze(text, AnalysisOptions(includeSpelling = true, includeGrammar = true))

        assertFalse("Must find corrections", corrections.isEmpty())
        for (c in corrections) {
            assertTrue("startOffset >= 0", c.startOffset >= 0)
            assertTrue("endOffset <= text.length", c.endOffset <= text.length)
            assertTrue("startOffset < endOffset", c.startOffset < c.endOffset)
            assertEquals("Substring must match originalText", c.originalText, text.substring(c.startOffset, c.endOffset))
        }

        val hasGo = corrections.any { it.originalText == "go" && it.suggestedReplacement == "goes" }
        assertTrue("Must correct 'go' -> 'goes'", hasGo)

        val hasRecieved = corrections.any { it.originalText == "recieved" && it.suggestedReplacement == "received" }
        assertTrue("Must correct 'recieved' -> 'received'", hasRecieved)
    }

    @Test
    fun testCredentialEncryptionSecurity() {
        val crypto = CryptoManager()
        val plainApiKey = "AIzaSyD-production-validation-key"
        val encrypted = crypto.encrypt(plainApiKey)

        assertNotEquals(plainApiKey, encrypted)
        val decrypted = crypto.decrypt(encrypted)
        assertEquals(plainApiKey, decrypted)
    }

    @Test
    fun testGectorEngineGracefulDegradation() = runBlocking {
        val engine = GectorOnnxEngine(File("non_existent.onnx"))
        assertFalse(engine.isAvailable())
        val corrections = engine.check("Testing graceful fallback.")
        assertTrue(corrections.isEmpty())
    }

    @Test
    fun testCoordinatorSinglePassEfficiency() = runBlocking {
        val facade = OfflineGrammarEngineFacade(null)
        val spellEngine = TrieSpellEngine(null, facade)
        val grammarEngine = RuleBasedGrammarEngine(null, facade)
        val coordinator = CorrectionCoordinator(spellEngine, grammarEngine)

        val text = "Their going to the the park."
        val corrections = coordinator.checkText(text)

        assertTrue("Must detect repeated words or confused words", corrections.isNotEmpty())
        for (c in corrections) {
            assertEquals(c.originalText, text.substring(c.startOffset, c.endOffset))
        }
    }
}
