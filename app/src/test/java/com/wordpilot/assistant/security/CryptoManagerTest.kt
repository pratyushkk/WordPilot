package com.wordpilot.assistant.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        cryptoManager = CryptoManager()
    }

    @Test
    fun testEncryptDecryptRoundTrip() {
        val sampleApiKey = "AIzaSyD-sample_gemini_api_key_1234567890"
        val encrypted = cryptoManager.encrypt(sampleApiKey)

        assertNotNull("Encrypted text must not be null", encrypted)
        assertNotEquals("Encrypted text must differ from plaintext", sampleApiKey, encrypted)

        val decrypted = cryptoManager.decrypt(encrypted)
        assertEquals("Decrypted text must match original plaintext", sampleApiKey, decrypted)
    }

    @Test
    fun testEmptyAndBlankHandling() {
        val empty = cryptoManager.encrypt("")
        assertEquals("", empty)

        assertNull(cryptoManager.decrypt(""))
        assertNull(cryptoManager.decrypt("   "))
    }

    @Test
    fun testLegacyUnencryptedFallback() {
        val plain = "unencrypted_legacy_key"
        val decrypted = cryptoManager.decrypt(plain)
        assertEquals(plain, decrypted)
    }
}
