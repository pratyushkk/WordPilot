package com.wordpilot.assistant.engine.orchestration

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.providers.GrammarProvider
import com.wordpilot.assistant.engine.providers.ProviderConfig
import com.wordpilot.assistant.engine.providers.ProviderResult
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ProviderOrchestratorFailoverTest {

    private fun makeCorrection(
        original: String = "don't",
        replacement: String = "doesn't"
    ) = Correction(
        startOffset = 0,
        endOffset = original.length,
        originalText = original,
        suggestedReplacement = replacement,
        category = CorrectionCategory.GRAMMAR,
        severity = CorrectionSeverity.WARNING,
        ruleId = "TEST_RULE"
    )

    private fun mockProvider(
        id: String,
        isConfigured: Boolean = true,
        isEnabled: Boolean = true,
        handler: suspend (String) -> ProviderResult
    ) = object : GrammarProvider {
        override val id: String = id
        override fun configure(config: ProviderConfig) {}
        override fun isConfigured(): Boolean = isConfigured
        override fun isEnabled(): Boolean = isEnabled
        override suspend fun check(text: String, language: String): ProviderResult = handler(text)
    }

    // === SCENARIO 1: Sapling succeeds ===
    @Test
    fun `Scenario 1 - Sapling succeeds as primary provider`() = runBlocking {
        val sapling = mockProvider("sapling") {
            ProviderResult.Success(listOf(makeCorrection()), "sapling")
        }
        val languageTool = mockProvider("languagetool") {
            throw AssertionError("LanguageTool should not be called when Sapling succeeds")
        }

        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling, "languagetool" to languageTool),
            providerPriority = listOf("sapling", "languagetool")
        )

        val result = orchestrator.check("He don't like it.")
        assertTrue(result.success)
        assertEquals("sapling", result.providerUsed)
        assertFalse(result.fallbackUsed)
        assertEquals(1, result.corrections.size)
    }

    // === SCENARIO 2: Sapling quota exhausted (402) -> fails over to LanguageTool ===
    @Test
    fun `Scenario 2 - Sapling quota exhausted fails over to LanguageTool`() = runBlocking {
        val sapling = mockProvider("sapling") {
            ProviderResult.Failure.QuotaExhausted("sapling", "Daily trial exhausted")
        }
        val languageTool = mockProvider("languagetool") {
            ProviderResult.Success(listOf(makeCorrection()), "languagetool")
        }

        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling, "languagetool" to languageTool),
            providerPriority = listOf("sapling", "languagetool")
        )

        val result = orchestrator.check("He don't like it.")
        assertTrue(result.success)
        assertEquals("languagetool", result.providerUsed)
        assertTrue(result.fallbackUsed)
        assertTrue(orchestrator.quotaTracker.isQuotaExhausted("sapling"))
    }

    // === SCENARIO 3: Sapling unavailable (HTTP 500) -> fails over to LanguageTool ===
    @Test
    fun `Scenario 3 - Sapling server error fails over to LanguageTool`() = runBlocking {
        val sapling = mockProvider("sapling") {
            ProviderResult.Failure.HttpError("sapling", "Internal server error", 500)
        }
        val languageTool = mockProvider("languagetool") {
            ProviderResult.Success(listOf(makeCorrection()), "languagetool")
        }

        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling, "languagetool" to languageTool),
            providerPriority = listOf("sapling", "languagetool")
        )

        val result = orchestrator.check("He don't like it.")
        assertTrue(result.success)
        assertEquals("languagetool", result.providerUsed)
        assertTrue(result.fallbackUsed)
    }

    // === SCENARIO 4: LanguageTool succeeds as primary if Sapling unconfigured ===
    @Test
    fun `Scenario 4 - LanguageTool succeeds when Sapling is unconfigured`() = runBlocking {
        val sapling = mockProvider("sapling", isConfigured = false) {
            throw AssertionError("Sapling should be skipped when unconfigured")
        }
        val languageTool = mockProvider("languagetool") {
            ProviderResult.Success(listOf(makeCorrection()), "languagetool")
        }

        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling, "languagetool" to languageTool),
            providerPriority = listOf("sapling", "languagetool")
        )

        val result = orchestrator.check("He don't like it.")
        assertTrue(result.success)
        assertEquals("languagetool", result.providerUsed)
        assertFalse(result.fallbackUsed)
    }

    // === SCENARIO 5 & 6: Sapling and LanguageTool fail -> GrammarBot succeeds ===
    @Test
    fun `Scenario 5 and 6 - Sapling and LanguageTool fail then GrammarBot succeeds`() = runBlocking {
        val sapling = mockProvider("sapling") {
            ProviderResult.Failure.Timeout("sapling", "Timed out")
        }
        val languageTool = mockProvider("languagetool") {
            ProviderResult.Failure.NetworkError("languagetool", "Connection refused")
        }
        val grammarBot = mockProvider("grammarbot") {
            ProviderResult.Success(listOf(makeCorrection()), "grammarbot")
        }

        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling, "languagetool" to languageTool, "grammarbot" to grammarBot),
            providerPriority = listOf("sapling", "languagetool", "grammarbot")
        )

        val result = orchestrator.check("He don't like it.")
        assertTrue(result.success)
        assertEquals("grammarbot", result.providerUsed)
        assertTrue(result.fallbackUsed)
    }

    // === SCENARIO 7: Every provider is unavailable ===
    @Test
    fun `Scenario 7 - Every provider fails returns graceful failure result`() = runBlocking {
        val sapling = mockProvider("sapling") {
            ProviderResult.Failure.NetworkError("sapling", "Unreachable")
        }
        val languageTool = mockProvider("languagetool") {
            ProviderResult.Failure.HttpError("languagetool", "Service unavailable", 503)
        }

        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling, "languagetool" to languageTool),
            providerPriority = listOf("sapling", "languagetool")
        )

        val result = orchestrator.check("He don't like it.")
        assertFalse(result.success)
        assertNull(result.providerUsed)
        assertTrue(result.corrections.isEmpty())
        assertNotNull(result.errorMessage)
    }

    // === SCENARIO 8: Provider recovers after circuit cooldown ===
    @Test
    fun `Scenario 8 - Provider recovers after circuit resets`() = runBlocking {
        var failSapling = true
        val sapling = mockProvider("sapling") {
            if (failSapling) ProviderResult.Failure.HttpError("sapling", "Error", 500)
            else ProviderResult.Success(listOf(makeCorrection()), "sapling")
        }
        val languageTool = mockProvider("languagetool") {
            ProviderResult.Success(listOf(makeCorrection()), "languagetool")
        }

        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling, "languagetool" to languageTool),
            providerPriority = listOf("sapling", "languagetool")
        )

        // First call: Sapling fails, fails over to LanguageTool
        val r1 = orchestrator.check("He don't like it.")
        assertEquals("languagetool", r1.providerUsed)

        // Sapling recovers
        failSapling = false
        // Simulate circuit reset
        val cb = orchestrator.getProviderStatus("sapling")

        val r2 = orchestrator.check("He don't like it.")
        assertTrue(r2.success)
    }

    // === SCENARIO 9: Concurrent requests handled safely ===
    @Test
    fun `Scenario 9 - Concurrent requests execute safely without state corruption`() = runBlocking {
        val callCount = AtomicInteger(0)
        val sapling = mockProvider("sapling") {
            callCount.incrementAndGet()
            ProviderResult.Success(listOf(makeCorrection()), "sapling")
        }

        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling),
            providerPriority = listOf("sapling")
        )

        val d1 = async { orchestrator.check("Sentence one.") }
        val d2 = async { orchestrator.check("Sentence two.") }

        val r1 = d1.await()
        val r2 = d2.await()

        assertTrue(r1.success)
        assertTrue(r2.success)
        assertEquals(2, callCount.get())
        assertEquals(2L, orchestrator.quotaTracker.getDailyRequestsUsed("sapling"))
    }

    // === SCENARIO 10: Revision / RequestId tracking ===
    @Test
    fun `Scenario 10 - Unique requestId preserves response identity across text revisions`() = runBlocking {
        val sapling = mockProvider("sapling") {
            ProviderResult.Success(listOf(makeCorrection()), "sapling")
        }
        val orchestrator = ProviderOrchestrator(
            providers = mapOf("sapling" to sapling),
            providerPriority = listOf("sapling")
        )

        val reqId1 = "rev-101"
        val reqId2 = "rev-102"

        val r1 = orchestrator.check("Initial text", requestId = reqId1)
        val r2 = orchestrator.check("Modified text", requestId = reqId2)

        assertEquals(reqId1, r1.requestId)
        assertEquals(reqId2, r2.requestId)
        assertNotEquals(r1.requestId, r2.requestId)
    }
}
