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

class GrammarBotProviderContractTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun createProvider(): GrammarBotProvider {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
        return GrammarBotProvider(client, server.url("/v1/check").toString())
    }

    @Test
    fun `disabled by default returns Disabled failure`() = runBlocking {
        val provider = createProvider()
        provider.configure(ProviderConfig("grammarbot", false, "some-key", null, priority = 3))
        val result = provider.check("I has an idea")
        assertTrue(result is ProviderResult.Failure.Disabled)
    }

    @Test
    fun `missing key returns NotConfigured failure`() = runBlocking {
        val provider = createProvider()
        provider.configure(ProviderConfig("grammarbot", true, "", null, priority = 3))
        val result = provider.check("I has an idea")
        assertTrue(result is ProviderResult.Failure.NotConfigured)
    }

    @Test
    fun `successful correction parses diff correctly`() = runBlocking {
        val provider = createProvider()
        provider.configure(ProviderConfig("grammarbot", true, "test-rapidapi-key", null, priority = 3))

        val mockJson = """
        {
            "status": "success",
            "correction": "I have an idea"
        }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val result = provider.check("I has an idea")
        assertTrue("Expected Success, got $result", result is ProviderResult.Success)

        val success = result as ProviderResult.Success
        assertEquals(1, success.corrections.size)
        val c = success.corrections[0]
        assertEquals("has", c.originalText)
        assertEquals("have", c.suggestedReplacement)
        assertEquals(2, c.startOffset)
        assertEquals(5, c.endOffset)
    }

    @Test
    fun `HTTP 401 returns AuthError`() = runBlocking {
        val provider = createProvider()
        provider.configure(ProviderConfig("grammarbot", true, "invalid-key", null, priority = 3))

        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message": "Invalid API key"}"""))

        val result = provider.check("Testing")
        assertTrue(result is ProviderResult.Failure.AuthError)
        assertEquals(401, (result as ProviderResult.Failure.AuthError).statusCode)
    }
}
