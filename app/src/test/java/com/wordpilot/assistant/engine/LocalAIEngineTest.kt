package com.wordpilot.assistant.engine

import com.wordpilot.assistant.data.repository.ModelRepositoryImpl
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.engine.impl.LocalAIEngineImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalAIEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var modelDir: File
    private lateinit var modelRepository: ModelRepositoryImpl
    private lateinit var aiEngine: LocalAIEngineImpl

    @Before
    fun setup() {
        modelDir = tempFolder.newFolder("models")
        modelRepository = ModelRepositoryImpl(modelDir)
        aiEngine = LocalAIEngineImpl(modelRepository)
    }

    @Test
    fun testModelInfoMetadata() {
        val info = aiEngine.modelInfo
        assertNotNull(info)
        assertEquals("Meta Llama 3.2 (1B Instruct)", info.name)
        assertTrue(info.source.contains("Hugging Face"))
        assertEquals(770L, info.downloadSizeMb)
        assertFalse(info.isInstalled)
    }

    @Test
    fun testRewriteFailsGracefullyWhenNotInstalled() = runBlocking {
        val result = aiEngine.rewrite("Hey this is not good", RewriteStyle.MAKE_PROFESSIONAL)
        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Meta Llama 3.2"))
    }

    @Test
    fun testRewriteEmptyTextSucceedsImmediately() = runBlocking {
        val result = aiEngine.rewrite("   ", RewriteStyle.MAKE_CONCISE)
        assertTrue(result.isSuccess)
        assertEquals("   ", result.rewrittenText)
    }

    @Test
    fun testExplainErrorsFormatting() = runBlocking {
        val corrections = listOf(
            Correction(
                startOffset = 0,
                endOffset = 3,
                originalText = "teh",
                suggestedReplacement = "the",
                category = CorrectionCategory.SPELLING,
                explanation = "Common typo for 'the'",
                severity = CorrectionSeverity.ERROR,
                ruleId = "SPELL_01"
            )
        )
        val explanation = aiEngine.explainErrors("teh book", corrections)
        assertTrue(explanation.contains("Llama 3.2"))
        assertTrue(explanation.contains("teh"))
        assertTrue(explanation.contains("the"))
        assertTrue(explanation.contains("Spelling"))
    }

    @Test
    fun testResourceUsageWhenNotInstalled() {
        val usage = aiEngine.getResourceUsage()
        assertEquals(0L, usage.memoryUsageMb)
        assertFalse(usage.isModelLoaded)
    }
}
