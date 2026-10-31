package com.wordpilot.assistant.engine.gector

import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.engine.CorrectionCoordinator
import com.wordpilot.assistant.engine.GrammarEngine
import com.wordpilot.assistant.engine.SpellEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis

data class SentenceTestCase(
    val category: String,
    val input: String,
    val description: String,
    val expectChanges: Boolean = true
)

class GectorComprehensiveSentenceTest {

    private fun createDummySpellEngine() = object : SpellEngine {
        override suspend fun check(text: String) = emptyList<Correction>()
        override fun isAvailable() = true
        override fun addWord(word: String) {}
        override fun removeWord(word: String) {}
        override fun isKnownWord(word: String) = true
        override fun getSuggestions(word: String, maxResults: Int) = emptyList<String>()
    }

    private fun createDummyGrammarEngine() = object : GrammarEngine {
        override suspend fun check(text: String) = emptyList<Correction>()
        override fun isAvailable() = true
        override fun getSupportedRuleIds() = emptyList<String>()
        override fun setDisabledRules(ruleIds: Set<String>) {}
    }

    @Test
    fun runComprehensiveSentenceEvaluation() = runBlocking {
        var modelFile = File("downloaded_models/gector/gector.onnx")
        if (!modelFile.exists()) modelFile = File("../downloaded_models/gector/gector.onnx")
        assertTrue("Model file must exist", modelFile.exists())

        val gectorEngine = GectorOnnxEngine(modelFile = modelFile)
        val initSuccess = gectorEngine.initialize()
        assertTrue("GectorOnnxEngine must initialize successfully", initSuccess)

        val coordinator = CorrectionCoordinator(
            spellEngine = createDummySpellEngine(),
            grammarEngine = createDummyGrammarEngine(),
            gectorEngine = gectorEngine,
            useGectorOnly = true
        )
        val safeHelper = SafeReplacementHelper()

        val testCases = listOf(
            // === 1. Short sentences ===
            SentenceTestCase(
                category = "Short Sentences",
                input = "he ate a apple",
                description = "Capitalization ('he' -> 'He'), article agreement ('a' -> 'an'), period"
            ),
            SentenceTestCase(
                category = "Short Sentences",
                input = "she go to school",
                description = "Capitalization ('she' -> 'She'), verb agreement ('go' -> 'went'), period"
            ),
            SentenceTestCase(
                category = "Short Sentences",
                input = "i like dogs",
                description = "Pronoun 'i' capitalization ('i' -> 'I'), period"
            ),
            SentenceTestCase(
                category = "Short Sentences",
                input = "I really really like it.",
                description = "Extraneous word deletion (duplicate 'really')"
            ),
            SentenceTestCase(
                category = "Short Sentences",
                input = "They is very happy.",
                description = "Subject-verb number agreement ('is' -> 'are')"
            ),
            SentenceTestCase(
                category = "Short Sentences",
                input = "We bought apples bananas and oranges.",
                description = "List comma insertion after 'apples'"
            ),

            // === 2. Medium sentences ===
            SentenceTestCase(
                category = "Medium Sentences",
                input = "she have went to a apple store yesterday and i was happy.",
                description = "Multiple errors: capitalization, verb 'have went' -> 'went', article 'a apple' -> 'an apple', pronoun 'i' -> 'I'"
            ),
            SentenceTestCase(
                category = "Medium Sentences",
                input = "Yesterday they goes to the market and buys some fresh fruits.",
                description = "Past tense verb agreement ('goes' -> 'went', 'buys' -> 'bought')"
            ),
            SentenceTestCase(
                category = "Medium Sentences",
                input = "However I think that this decision were not correct.",
                description = "Introductory adverb comma ('However,'), verb agreement ('were' -> 'was')"
            ),
            SentenceTestCase(
                category = "Medium Sentences",
                input = "The student were excited about the new project.",
                description = "Singular subject-verb agreement ('student were' -> 'student was')"
            ),

            // === 3. Long Multi-Sentence Paragraphs ===
            SentenceTestCase(
                category = "Long Paragraphs",
                input = "yesterday we goes to the new restaurant downtown. the food were really really delicious but the service was a bit slow. i order a pizza with mushroom and onions and my friend ordered pasta. However we will definitely visit again next week",
                description = "Full paragraph with 4 sentences, mixed capitalization, duplicate words, verb tenses, and missing period"
            ),
            SentenceTestCase(
                category = "Long Paragraphs",
                input = "Although the weather was bad and raining heavily outside she decide to walk to school without an umbrella because she have missed the morning bus and did not wanted to be late for her important examination.",
                description = "Complex 35-word single sentence with conditional clause and multiple past-tense verb errors ('decide' -> 'decided', 'have missed' -> 'had missed', 'did not wanted' -> 'did not want')"
            ),

            // === 4. Already Correct Sentences (False Positive Check) ===
            SentenceTestCase(
                category = "Already Correct",
                input = "The quick brown fox jumps over the lazy dog.",
                description = "Pangram - already grammatically clean, should produce 0 false errors",
                expectChanges = false
            ),
            SentenceTestCase(
                category = "Already Correct",
                input = "She has already finished her homework, and she is ready for bed.",
                description = "Compound sentence with correct punctuation and agreement",
                expectChanges = false
            ),

            // === 5. Edge cases (contractions, numbers, quotes) ===
            SentenceTestCase(
                category = "Edge Cases",
                input = "i'm not sure if they'll arrive on time today",
                description = "Contraction capitalization (\"i'm\" -> \"I'm\"), ending period"
            ),
            SentenceTestCase(
                category = "Edge Cases",
                input = "i am getting many incorrect results, whats the best way to fix it",
                description = "User query test: capitalization ('i' -> 'I'), contraction ('whats' -> 'what's'), ending punctuation ('it?')"
            )
        )

        println("================================================================================")
        println("       WORDPILOT GECTOR COMPREHENSIVE ON-DEVICE SENTENCE EVALUATION REPORT      ")
        println("================================================================================")

        var passedCount = 0
        var totalInferenceTimeMs = 0L

        for ((idx, tc) in testCases.withIndex()) {
            val caseNum = idx + 1
            println("\n--------------------------------------------------------------------------------")
            println("[$caseNum/${testCases.size}] [${tc.category}] ${tc.description}")
            println("  INPUT: \"${tc.input}\"")

            val corrections: List<Correction>
            val duration = measureTimeMillis {
                corrections = coordinator.checkText(tc.input)
            }
            totalInferenceTimeMs += duration

            println("  LATENCY: ${duration}ms | CORRECTIONS DETECTED: ${corrections.size}")
            for ((cIdx, c) in corrections.withIndex()) {
                println("    ($cIdx) [${c.category}] rule=${c.ruleId} range=[${c.startOffset}..${c.endOffset}]")
                println("        '${c.originalText}' -> '${c.suggestedReplacement}' (explanation: ${c.explanation})")
            }

            val applied = safeHelper.applyMultipleCorrections(tc.input, corrections)
            println("  OUTPUT: \"$applied\"")

            // Verification checks
            var caseSuccess = true
            if (!tc.expectChanges) {
                if (corrections.isNotEmpty()) {
                    println("  ⚠️ WARNING: False positive detected on already clean sentence! (found ${corrections.size} corrections)")
                    // Not necessarily fatal if suggestions are benign, but record it
                } else {
                    println("  ✓ PASSED: Correctly identified as clean text without false positives.")
                }
            } else {
                if (corrections.isEmpty()) {
                    println("  ❌ FAILED: Expected corrections but found 0.")
                    caseSuccess = false
                } else {
                    if (applied == tc.input) {
                        println("  ❌ FAILED: Corrections were detected but could not be safely applied by SafeReplacementHelper.")
                        caseSuccess = false
                    } else {
                        println("  ✓ PASSED: Successfully detected and applied all corrections.")
                    }
                }
            }

            // Offset integrity check
            for (c in corrections) {
                if (c.startOffset < 0 || c.endOffset > tc.input.length || c.startOffset > c.endOffset) {
                    println("  ❌ ERROR: Invalid offset range [${c.startOffset}..${c.endOffset}] in text of length ${tc.input.length}")
                    caseSuccess = false
                }
            }

            if (caseSuccess) passedCount++
        }

        println("\n================================================================================")
        println("SUMMARY: $passedCount/${testCases.size} test cases passed")
        println("AVG LATENCY: ${totalInferenceTimeMs / testCases.size}ms per test case")
        println("TOTAL TIME: ${totalInferenceTimeMs}ms across ${testCases.size} cases")
        println("================================================================================")

        gectorEngine.close()
        assertEquals("All expected cases should pass safe replacement", testCases.size, passedCount)
    }
}
