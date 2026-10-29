package com.wordpilot.assistant.utils

object TextUtils {
    fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun charCount(text: String): Int = text.length

    fun isUrl(word: String): Boolean {
        return word.startsWith("http://") || word.startsWith("https://") ||
            word.startsWith("www.") || word.contains("://")
    }

    fun isEmail(word: String): Boolean {
        return word.contains("@") && word.contains(".")
    }

    fun isCodeLike(word: String): Boolean {
        return word.contains("_") || word.contains("::") ||
            (word.length > 2 && word[0].isLowerCase() && word.any { it.isUpperCase() })
    }

    fun isAllCapsAbbreviation(word: String): Boolean {
        return word.length in 2..5 && word.all { it.isUpperCase() || it == '.' }
    }

    fun isNumeric(word: String): Boolean {
        return word.any { it.isDigit() }
    }

    fun shouldSkipWord(word: String): Boolean {
        return isUrl(word) || isEmail(word) || isCodeLike(word) ||
            isAllCapsAbbreviation(word) || isNumeric(word) || word.length <= 1
    }

    fun stripPunctuation(word: String): String {
        return word.trimStart { !it.isLetterOrDigit() }.trimEnd { !it.isLetterOrDigit() }
    }

    fun startsWithVowelSound(word: String): Boolean {
        if (word.isEmpty()) return false
        val lower = word.lowercase()
        // Special cases
        val silentH = setOf("hour", "honor", "honest", "heir", "herb")
        if (silentH.any { lower.startsWith(it) }) return true
        // "uni-" words use "a" not "an"
        if (lower.startsWith("uni") && lower.length > 3 && lower[3].isLetter()) return false
        if (lower.startsWith("eu")) return false
        if (lower.startsWith("one") || lower.startsWith("once")) return false
        return lower[0] in setOf('a', 'e', 'i', 'o', 'u')
    }
}
