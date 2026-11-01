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

class SaplingProviderContractTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: SaplingProvider

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        provider = SaplingProvider(client)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun configureProvider(apiKey: String = "test-key-123", enabled: Boolean = true) {
        provider.configure(
            ProviderConfig(
                id = "sapling",
                enabled = enabled,
                apiKey = apiKey,
                baseUrl = null,
                priority = 1
            )
        )
    }

    @Test
    fun `disabled provider returns Disabled failure`() = runBlocking {
        configureProvider(enabled = false)
        val result = provider.check("Some text")
        assertTrue(result is ProviderResult.Failure.Disabled)
    }

    @Test
    fun `missing API key returns NotConfigured failure`() = runBlocking {
        configureProvider(apiKey = "")
        val result = provider.check("Some text")
        assertTrue(result is ProviderResult.Failure.NotConfigured)
    }

    @Test
    fun `valid response parses sentence-relative offsets to absolute document offsets`() = runBlocking {
        // Mock server handles standard edits response
        val mockJson = """
        {
            "applied_text": "He doesn't like it.",
            "edits": [
                {
                    "sentence": "He don't like it.",
                    "sentence_start": 0,
                    "start": 3,
                    "end": 8,
                    "replacement": "doesn't",
                    "error_type": "Subject-Verb Agreement",
                    "general_error_type": "Grammar"
                }
            ],
            "degraded": false
        }
        """.trimIndent()

        // Using helper that injects URL
        val testProvider = createTestSaplingProvider(server.url("/api/v1/edits").toString())
        testProvider.configure(ProviderConfig("sapling", true, "test-key", null, priority = 1))

        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val result = testProvider.check("He don't like it.")
        assertTrue("Expected Success, got $result", result is ProviderResult.Success)

        val success = result as ProviderResult.Success
        assertEquals(1, success.corrections.size)
        val c = success.corrections[0]
        assertEquals(3, c.startOffset)
        assertEquals(8, c.endOffset)
        assertEquals("don't", c.originalText)
        assertEquals("doesn't", c.suggestedReplacement)
        assertEquals("sapling", success.providerId)
        assertTrue(success.isComplete)
    }

    @Test
    fun `HTTP 429 returns RateLimited with Retry-After header`() = runBlocking {
        val testProvider = createTestSaplingProvider(server.url("/api/v1/edits").toString())
        testProvider.configure(ProviderConfig("sapling", true, "test-key", null, priority = 1))

        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "30")
                .setBody("""{"message": "Rate limit exceeded"}""")
        )

        val result = testProvider.check("Testing text")
        assertTrue("Expected RateLimited, got $result", result is ProviderResult.Failure.RateLimited)
        val failure = result as ProviderResult.Failure.RateLimited
        assertEquals(30L, failure.retryAfterSeconds)
    }

    @Test
    fun `HTTP 402 returns QuotaExhausted`() = runBlocking {
        val testProvider = createTestSaplingProvider(server.url("/api/v1/edits").toString())
        testProvider.configure(ProviderConfig("sapling", true, "test-key", null, priority = 1))

        server.enqueue(
            MockResponse()
                .setResponseCode(402)
                .setBody("""{"message": "Trial quota exhausted"}""")
        )

        val result = testProvider.check("Testing text")
        assertTrue("Expected QuotaExhausted, got $result", result is ProviderResult.Failure.QuotaExhausted)
    }

    @Test
    fun `HTTP 401 returns AuthError`() = runBlocking {
        val testProvider = createTestSaplingProvider(server.url("/api/v1/edits").toString())
        testProvider.configure(ProviderConfig("sapling", true, "bad-key", null, priority = 1))

        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Invalid API key"}""")
        )

        val result = testProvider.check("Testing text")
        assertTrue("Expected AuthError, got $result", result is ProviderResult.Failure.AuthError)
        assertEquals(401, (result as ProviderResult.Failure.AuthError).statusCode)
    }

    @Test
    fun `malformed JSON returns InvalidResponse`() = runBlocking {
        val testProvider = createTestSaplingProvider(server.url("/api/v1/edits").toString())
        testProvider.configure(ProviderConfig("sapling", true, "test-key", null, priority = 1))

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("THIS IS NOT JSON {{{")
        )

        val result = testProvider.check("Testing text")
        assertTrue("Expected InvalidResponse, got $result", result is ProviderResult.Failure.InvalidResponse)
    }

    @Test
    fun `empty edits array returns Success with empty corrections list`() = runBlocking {
        val mockJson = """{"edits": [], "degraded": false}"""
        val testProvider = createTestSaplingProvider(server.url("/api/v1/edits").toString())
        testProvider.configure(ProviderConfig("sapling", true, "test-key", null, priority = 1))

        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val result = testProvider.check("The quick brown fox jumps over the lazy dog.")
        assertTrue("Expected Success, got $result", result is ProviderResult.Success)
        assertTrue((result as ProviderResult.Success).corrections.isEmpty())
    }

    private fun createTestSaplingProvider(endpointUrl: String): SaplingProvider {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        return SaplingProvider(client, endpointUrl)
    }
}
