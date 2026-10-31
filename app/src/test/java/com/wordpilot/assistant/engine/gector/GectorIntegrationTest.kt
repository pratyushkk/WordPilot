package com.wordpilot.assistant.engine.gector

import com.wordpilot.assistant.domain.models.CorrectionCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GectorIntegrationTest {

    @Test
    fun testTokenizerProducesValidTokenSpans() {
        val text = "She go to school."
        val tokenizer = GectorTokenizer()
        val tokenized = tokenizer.tokenize(text)

        assertEquals(5, tokenized.tokens.size) // ["She", "go", "to", "school", "."]
        for (span in tokenized.tokens) {
            val extracted = text.substring(span.startOffset, span.endOffset)
            assertEquals(span.token, extracted)
        }

        assertEquals(GectorTokenizer.CLS_ID, tokenized.inputIds.first())
        assertEquals(GectorTokenizer.SEP_ID, tokenized.inputIds.last())
        assertEquals(tokenized.tokens.size + 2, tokenized.inputIds.size)
    }

    @Test
    fun testTagDecoderGeneratesValidCorrections() {
        val text = "She go to school."
        val tokenizer = GectorTokenizer()
        val tokenized = tokenizer.tokenize(text)

        val decoder = GectorTagDecoder()
        val predictedTags = listOf(
            GectorTagDecoder.TAG_KEEP,       // She
            "\$REPLACE_goes",                // go -> goes
            GectorTagDecoder.TAG_KEEP,       // to
            GectorTagDecoder.TAG_KEEP,       // school
            GectorTagDecoder.TAG_KEEP        // .
        )

        val corrections = decoder.decodeTags(tokenized.tokens, predictedTags, text)
        assertEquals(1, corrections.size)

        val c = corrections.first()
        assertEquals("go", c.originalText)
        assertEquals("goes", c.suggestedReplacement)
        assertEquals(4, c.startOffset)
        assertEquals(6, c.endOffset)
        assertEquals("go", text.substring(c.startOffset, c.endOffset))
        assertEquals(CorrectionCategory.GRAMMAR, c.category)
    }

    @Test
    fun testTagDecoderHandlesDeleteAndAppend() {
        val text = "I really really like it."
        val tokenizer = GectorTokenizer()
        val tokenized = tokenizer.tokenize(text)

        val decoder = GectorTagDecoder()
        val predictedTags = listOf(
            GectorTagDecoder.TAG_KEEP,
            GectorTagDecoder.TAG_KEEP,
            GectorTagDecoder.TAG_DELETE,    // delete second "really"
            GectorTagDecoder.TAG_KEEP,
            GectorTagDecoder.TAG_KEEP,
            GectorTagDecoder.TAG_KEEP
        )

        val corrections = decoder.decodeTags(tokenized.tokens, predictedTags, text)
        val deleteCorrection = corrections.find { it.ruleId == "GECTOR_DELETE" }
        assertNotNull("Should emit deletion correction", deleteCorrection)
        assertTrue(deleteCorrection!!.originalText.startsWith("really"))
        assertEquals("", deleteCorrection.suggestedReplacement)
    }

    @Test
    fun testOnnxEngineGracefulFallbackWhenModelAbsent() = runBlocking {
        val dummyFile = File("non_existent_gector_model.onnx")
        val engine = GectorOnnxEngine(modelFile = dummyFile)

        assertFalse("Engine must not be available when file is absent", engine.isAvailable())
        val initResult = engine.initialize()
        assertFalse("Initialization must return false when file is absent", initResult)
        assertFalse(engine.isAvailable())

        val corrections = engine.check("She go to school.")
        assertTrue("Must return empty list when unavailable", corrections.isEmpty())
    }
}
