package com.wordpilot.assistant.engine.gector

import com.wordpilot.assistant.domain.models.CorrectionCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GectorIntegrationTest {

    @Test
    fun testRealGectorEngineEndToEndCorrection() = runBlocking {
        var modelFile = File("downloaded_models/gector/gector.onnx")
        if (!modelFile.exists()) modelFile = File("../downloaded_models/gector/gector.onnx")
        if (!modelFile.exists()) return@runBlocking

        val engine = GectorOnnxEngine(modelFile = modelFile)
        val initialized = engine.initialize()
        assertTrue("Engine must initialize with real assets", initialized)
        assertTrue("Engine must be available", engine.isAvailable())

        // Test 1: Grammar agreement error
        val s1 = "She go to school."
        val corr1 = engine.check(s1)
        println("=== Real GECToR Corrections on '$s1' ===")
        for (c in corr1) {
            println("Correction: '${c.originalText}' -> '${c.suggestedReplacement}' [${c.startOffset}..${c.endOffset}] (${c.category}, ${c.ruleId})")
        }
        assertTrue("Must detect grammar error on 'go'", corr1.isNotEmpty())
        assertEquals("go", corr1.first().originalText)
        assertTrue(corr1.first().suggestedReplacement == "went" || corr1.first().suggestedReplacement == "goes")

        // Test 2: Deletion of redundant word
        val s2 = "I really really like it."
        val corr2 = engine.check(s2)
        println("=== Real GECToR Corrections on '$s2' ===")
        for (c in corr2) {
            println("Correction: '${c.originalText}' -> '${c.suggestedReplacement}' [${c.startOffset}..${c.endOffset}] (${c.category}, ${c.ruleId})")
        }

        // Test 3: Article agreement
        val s3 = "He ate a apple."
        val corr3 = engine.check(s3)
        println("=== Real GECToR Corrections on '$s3' ===")
        for (c in corr3) {
            println("Correction: '${c.originalText}' -> '${c.suggestedReplacement}' [${c.startOffset}..${c.endOffset}] (${c.category}, ${c.ruleId})")
        }

        engine.close()
    }
}
