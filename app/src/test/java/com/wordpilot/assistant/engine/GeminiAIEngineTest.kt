package com.wordpilot.assistant.engine

import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.engine.impl.GeminiAIEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeminiAIEngineTest {

    @Test
    fun testSanitizeApiKey_handlesVariousFormats() {
        // Clean standard key
        assertEquals("AIzaSyD-1234567890abcdef", GeminiAIEngine.sanitizeApiKey("AIzaSyD-1234567890abcdef"))

        // Key with leading and trailing spaces
        assertEquals("AIzaSyD-1234567890abcdef", GeminiAIEngine.sanitizeApiKey("   AIzaSyD-1234567890abcdef   "))

        // Key with newlines and carriage returns (common clipboard artifact)
        assertEquals("AIzaSyD-1234567890abcdef", GeminiAIEngine.sanitizeApiKey("\r\n  AIzaSyD-1234567890abcdef \n\r"))

        // Key enclosed in double quotes
        assertEquals("AIzaSyD-1234567890abcdef", GeminiAIEngine.sanitizeApiKey("\"AIzaSyD-1234567890abcdef\""))

        // Key enclosed in single quotes
        assertEquals("AIzaSyD-1234567890abcdef", GeminiAIEngine.sanitizeApiKey("'AIzaSyD-1234567890abcdef'"))

        // Key with 'Bearer ' prefix
        assertEquals("AIzaSyD-1234567890abcdef", GeminiAIEngine.sanitizeApiKey("Bearer AIzaSyD-1234567890abcdef"))

        // Key with 'key=' prefix
        assertEquals("AIzaSyD-1234567890abcdef", GeminiAIEngine.sanitizeApiKey("key=AIzaSyD-1234567890abcdef"))

        // Blank and null handling
        assertEquals("", GeminiAIEngine.sanitizeApiKey(null))
        assertEquals("", GeminiAIEngine.sanitizeApiKey(""))
        assertEquals("", GeminiAIEngine.sanitizeApiKey("   \n\t  "))
    }

    @Test
    fun testModelInfo_isGeminiModel() {
        val engine = GeminiAIEngine { "AIzaSyTest" }
        assertEquals("Gemini Model", engine.modelInfo.name)
        assertTrue(engine.isModelInstalled)
    }

    @Test
    fun testIsModelInstalled_reflectsKeyPresence() {
        val engineWithoutKey = GeminiAIEngine { "" }
        assertFalse(engineWithoutKey.isModelInstalled)

        val engineWithKey = GeminiAIEngine { "AIzaSyValidKey" }
        assertTrue(engineWithKey.isModelInstalled)

        val engineWithPaddedKey = GeminiAIEngine { "   AIzaSyValidKey \n " }
        assertTrue(engineWithPaddedKey.isModelInstalled)
    }

    @Test
    fun testRewriteFailsCleanlyWhenKeyMissing() = runBlocking {
        val engine = GeminiAIEngine { null }
        val result = engine.rewrite("Hello world", RewriteStyle.MAKE_PROFESSIONAL)
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage!!.contains("Gemini API key is not configured"))
    }

    @Test
    fun testBuildPrompt_containsToneInstructions() {
        val engine = GeminiAIEngine { "key" }

        val professionalPrompt = engine.buildPrompt("I want money", RewriteStyle.MAKE_PROFESSIONAL, null)
        assertTrue(professionalPrompt.contains("polished, respectful, executive business tone"))
        assertTrue(professionalPrompt.contains("\"I want money\""))

        val concisePrompt = engine.buildPrompt("In order to achieve this we should do it", RewriteStyle.MAKE_CONCISE, null)
        assertTrue(concisePrompt.contains("concise and direct"))

        val casualPrompt = engine.buildPrompt("Greetings esteemed colleague", RewriteStyle.MAKE_CASUAL, null)
        assertTrue(concisePrompt.isNotBlank())
        assertTrue(casualPrompt.contains("relaxed, modern, casual"))

        val customPrompt = engine.buildPrompt("Hey", RewriteStyle.CUSTOM, "Make it sound like Shakespeare")
        assertTrue(customPrompt.contains("Shakespeare"))
    }

    @Test
    fun testParseGeminiOutput_parsesValidJson() {
        val engine = GeminiAIEngine { "key" }
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Dear team, please find the updated report attached."
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
        """.trimIndent()

        val parsed = engine.parseGeminiOutput(json)
        assertEquals("Dear team, please find the updated report attached.", parsed)
    }

    @Test
    fun testCleanRewrittenText_stripsQuotesAndBackticks() {
        val engine = GeminiAIEngine { "key" }

        assertEquals("Hello world", engine.cleanRewrittenText("\"Hello world\"", "Hello"))
        assertEquals("Hello world", engine.cleanRewrittenText("“Hello world”", "Hello"))
        assertEquals("Hello world", engine.cleanRewrittenText("```Hello world```", "Hello"))
        assertEquals("Original", engine.cleanRewrittenText("   ", "Original"))
    }

    @Test
    fun testParseErrorMessage_identifiesSpecificErrors() {
        val engine = GeminiAIEngine { "key" }

        // Invalid key
        val keyErrorJson = """
            {
              "error": {
                "code": 400,
                "message": "API key not valid. Please pass a valid API key.",
                "status": "INVALID_ARGUMENT",
                "details": [
                  {
                    "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                    "reason": "API_KEY_INVALID"
                  }
                ]
              }
            }
        """.trimIndent()
        val errorMsg = engine.parseErrorMessage(keyErrorJson, 400)
        assertTrue(errorMsg.contains("Invalid Gemini API key"))

        // Quota exceeded (429)
        val quotaJson = """
            {
              "error": {
                "code": 429,
                "message": "Resource exhausted",
                "status": "RESOURCE_EXHAUSTED"
              }
            }
        """.trimIndent()
        val quotaMsg = engine.parseErrorMessage(quotaJson, 429)
        assertTrue(quotaMsg.contains("quota exceeded"))
    }
}
