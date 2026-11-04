package com.wordpilot.assistant.engine.providers

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class LanguageToolProviderContractTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: LanguageToolProvider

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        provider = LanguageToolProvider(client)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `disabled provider returns Disabled failure`() = runBlocking {
        provider.configure(ProviderConfig("languagetool", false, null, server.url("/").toString(), priority = 2))
        val result = provider.check("Some text")
        assertTrue(result is ProviderResult.Failure.Disabled)
    }

    @Test
    fun `missing baseUrl returns NotConfigured failure`() = runBlocking {
        provider.configure(ProviderConfig("languagetool", true, null, "", priority = 2))
        val result = provider.check("Some text")
        assertTrue(result is ProviderResult.Failure.NotConfigured)
    }

    @Test
    fun `successful check parses matches to Corrections with correct offset and length`() = runBlocking {
        val mockJson = """
        {
            "software": {"name": "LanguageTool", "version": "6.4"},
            "matches": [
                {
                    "message": "Possible spelling mistake found.",
                    "shortMessage": "Spelling mistake",
                    "offset": 0,
                    "length": 5,
                    "replacements": [{"value": "There"}],
                    "rule": {
                        "id": "MORFOLOGIK_RULE_EN_US",
                        "description": "English spellchecker",
                        "category": {"id": "TYPOS", "name": "Possible Typo"}
                    }
                }
            ]
        }
        """.trimIndent()

        provider.configure(ProviderConfig("languagetool", true, null, server.url("/").toString(), priority = 2))
        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val result = provider.check("Therr is a dog.")
        assertTrue("Expected Success, got $result", result is ProviderResult.Success)

        val success = result as ProviderResult.Success
        assertEquals(1, success.corrections.size)
        val c = success.corrections[0]
        assertEquals(0, c.startOffset)
        assertEquals(5, c.endOffset)
        assertEquals("Therr", c.originalText)
        assertEquals("There", c.suggestedReplacement)
        assertEquals("LT_MORFOLOGIK_RULE_EN_US", c.ruleId)
    }

    @Test
    fun `empty matches returns Success with empty list`() = runBlocking {
        val mockJson = """{"matches": []}"""
        provider.configure(ProviderConfig("languagetool", true, null, server.url("/").toString(), priority = 2))
        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val result = provider.check("The dog is good.")
        assertTrue("Expected Success, got $result", result is ProviderResult.Success)
        assertTrue((result as ProviderResult.Success).corrections.isEmpty())
    }

    @Test
    fun `HTTP 429 returns RateLimited`() = runBlocking {
        provider.configure(ProviderConfig("languagetool", true, null, server.url("/").toString(), priority = 2))
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "10").setBody("Rate limited"))

        val result = provider.check("Some text")
        assertTrue("Expected RateLimited, got $result", result is ProviderResult.Failure.RateLimited)
        assertEquals(10L, (result as ProviderResult.Failure.RateLimited).retryAfterSeconds)
    }

    @Test
    fun `HTTP 500 returns HttpError`() = runBlocking {
        provider.configure(ProviderConfig("languagetool", true, null, server.url("/").toString(), priority = 2))
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val result = provider.check("Some text")
        assertTrue("Expected HttpError, got $result", result is ProviderResult.Failure.HttpError)
        assertEquals(500, (result as ProviderResult.Failure.HttpError).statusCode)
    }
}
