package com.wordpilot.assistant.engine.gector

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Handles verb morphological transformations predicted by GECToR models
 * (e.g., $TRANSFORM_VERB_VB_VBZ, $TRANSFORM_VERB_VB_VBD).
 * Uses the canonical verb-form-vocab.txt mapping.
 */
class GectorVerbConjugator {
    companion object {
        private const val TAG = "GectorVerbConjugator"
    }

    // Key: "${sourceWord.lowercase()}:${rule}" -> targetWord
    private val conjugationMap = HashMap<String, String>(25000)

    var isLoaded: Boolean = false
        private set

    fun loadFromFile(file: File): Boolean {
        if (!file.exists() || !file.canRead()) return false
        return try {
            file.inputStream().use { loadFromStream(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load verb vocab from file", e)
            false
        }
    }

    fun loadFromStream(inputStream: InputStream): Boolean {
        return try {
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    // Format: source_target:RULE (e.g., abandon_abandons:VB_VBZ)
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx > 0 && colonIdx < trimmed.length - 1) {
                        val wordPair = trimmed.substring(0, colonIdx)
                        val rule = trimmed.substring(colonIdx + 1)
                        val underscoreIdx = wordPair.indexOf('_')
                        if (underscoreIdx > 0 && underscoreIdx < wordPair.length - 1) {
                            val source = wordPair.substring(0, underscoreIdx).lowercase()
                            val target = wordPair.substring(underscoreIdx + 1)
                            conjugationMap["$source:$rule"] = target
                        }
                    }
                }
            }
            isLoaded = true
            Log.d(TAG, "Loaded ${conjugationMap.size} verb conjugation rules")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse verb conjugation vocab", e)
            false
        }
    }

    /**
     * Conjugate a verb word according to the GECToR transformation rule
     * (e.g. word = "go", rule = "VB_VBZ" -> "goes").
     * Preserves capitalization.
     */
    fun conjugate(word: String, rule: String): String? {
        if (!isLoaded || word.isEmpty()) return null
        val lower = word.lowercase()
        val target = conjugationMap["$lower:$rule"] ?: return null

        // Preserve original capitalization
        return when {
            word.all { it.isUpperCase() } -> target.uppercase()
            word[0].isUpperCase() -> target.replaceFirstChar { it.uppercase() }
            else -> target
        }
    }
}
