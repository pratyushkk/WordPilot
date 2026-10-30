package com.wordpilot.assistant.engine.offline.normalizer

import com.wordpilot.assistant.engine.offline.model.NormalizedResult
import com.wordpilot.assistant.engine.offline.model.WordCasing

class TextNormalizer {

    fun normalize(rawText: String): NormalizedResult {
        if (rawText.isEmpty()) {
            return NormalizedResult("", IntArray(1) { 0 }, IntArray(1) { 0 })
        }

        val normBuilder = StringBuilder(rawText.length)
        val origToNorm = IntArray(rawText.length + 1)
        val normToOrig = ArrayList<Int>(rawText.length + 1)

        var origIdx = 0
        while (origIdx < rawText.length) {
            val ch = rawText[origIdx]
            val currentNormIdx = normBuilder.length
            origToNorm[origIdx] = currentNormIdx

            when (ch) {
                // Curly single quotes / apostrophes
                '’', '‘', 'ʼ', 'ʻ', '′', '`' -> {
                    normToOrig.add(origIdx)
                    normBuilder.append('\'')
                    origIdx++
                }
                // Curly double quotes
                '“', '”', '«', '»', '″' -> {
                    normToOrig.add(origIdx)
                    normBuilder.append('"')
                    origIdx++
                }
                // Ellipsis character
                '…' -> {
                    // Map all 3 dots back to the single original char index
                    normToOrig.add(origIdx)
                    normToOrig.add(origIdx)
                    normToOrig.add(origIdx)
                    normBuilder.append("...")
                    origIdx++
                }
                // Em/En dashes
                '—', '–', '―' -> {
                    normToOrig.add(origIdx)
                    normBuilder.append('-')
                    origIdx++
                }
                // Non-breaking spaces and exotic whitespace
                '\u00A0', '\u2007', '\u202F', '\uFEFF' -> {
                    normToOrig.add(origIdx)
                    normBuilder.append(' ')
                    origIdx++
                }
                // Zero-width space
                '\u200B', '\u200C', '\u200D' -> {
                    // Skip in normalized text, but keep map aligned
                    origIdx++
                }
                else -> {
                    normToOrig.add(origIdx)
                    normBuilder.append(ch)
                    origIdx++
                }
            }
        }

        // Final boundary marker
        origToNorm[rawText.length] = normBuilder.length
        normToOrig.add(rawText.length)

        return NormalizedResult(
            normalizedText = normBuilder.toString(),
            originalToNormalizedMap = origToNorm,
            normalizedToOriginalMap = normToOrig.toIntArray()
        )
    }

    companion object {
        fun detectCasing(word: String): WordCasing {
            if (word.isEmpty()) return WordCasing.LOWERCASE
            val letters = word.filter { it.isLetter() }
            if (letters.isEmpty()) return WordCasing.LOWERCASE

            val allUpper = letters.all { it.isUpperCase() }
            if (allUpper) return WordCasing.UPPERCASE

            val allLower = letters.all { it.isLowerCase() }
            if (allLower) return WordCasing.LOWERCASE

            val firstUpperRestLower = letters.first().isUpperCase() && letters.drop(1).all { it.isLowerCase() }
            if (firstUpperRestLower) return WordCasing.TITLECASE

            return WordCasing.MIXED
        }

        fun applyCasing(word: String, targetCasing: WordCasing): String = when (targetCasing) {
            WordCasing.LOWERCASE -> word.lowercase()
            WordCasing.UPPERCASE -> word.uppercase()
            WordCasing.TITLECASE -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            WordCasing.MIXED -> word
        }
    }
}
