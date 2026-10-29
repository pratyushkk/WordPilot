package com.wordpilot.assistant.engine.impl

import android.content.Context
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.SpellEngine
import com.wordpilot.assistant.utils.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class TrieSpellEngine(private val context: Context? = null) : SpellEngine {

    private class TrieNode {
        val children = ConcurrentHashMap<Char, TrieNode>()
        var isEndOfWord = false
        var frequency = 0
    }

    private val root = TrieNode()
    private val customWords = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var isInitialized = false

    init {
        // Pre-load common core words immediately
        loadCoreWords()
        // Load asset dictionary in background if context available
        if (context != null) {
            try {
                loadFromAssets(context)
            } catch (e: Exception) {
                // Fallback to core words
            }
        }
    }

    private fun loadCoreWords() {
        val core = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
            "assistant", "wordpilot", "grammarly", "keyboard", "android", "editor", "market",
            "school", "phone", "apple", "apples", "orange", "hello", "today", "tomorrow"
        )
        for (w in core) {
            insertWord(w, 100)
        }
    }

    private fun loadFromAssets(context: Context) {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("words.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            var rank = 10000
            while (reader.readLine().also { line = it } != null) {
                val word = line?.trim()?.lowercase() ?: continue
                if (word.isNotEmpty() && word.all { it.isLetter() || it == '\'' || it == '-' }) {
                    insertWord(word, rank)
                    if (rank > 1) rank--
                }
            }
            reader.close()
            isInitialized = true
        } catch (e: Exception) {
            isInitialized = true
        }
    }

    private fun insertWord(word: String, frequency: Int = 1) {
        var current = root
        for (ch in word.lowercase()) {
            current = current.children.computeIfAbsent(ch) { TrieNode() }
        }
        current.isEndOfWord = true
        current.frequency = maxOf(current.frequency, frequency)
    }

    override fun isAvailable(): Boolean = true

    override fun addWord(word: String) {
        val clean = word.trim().lowercase()
        if (clean.isNotEmpty()) {
            customWords.add(clean)
            insertWord(clean, 1000)
        }
    }

    override fun removeWord(word: String) {
        val clean = word.trim().lowercase()
        customWords.remove(clean)
        // Mark node as not end of word
        var current = root
        for (ch in clean) {
            current = current.children[ch] ?: return
        }
        current.isEndOfWord = false
    }

    override fun isKnownWord(word: String): Boolean {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return true
        if (clean in customWords) return true

        var current = root
        for (ch in clean) {
            current = current.children[ch] ?: return false
        }
        return current.isEndOfWord
    }

    override fun getSuggestions(word: String, maxResults: Int): List<String> {
        val target = word.trim().lowercase()
        if (target.isEmpty()) return emptyList()

        val results = mutableListOf<Pair<String, Int>>() // Pair of word to score

        // Trie-based Levenshtein search
        val currentRow = IntArray(target.length + 1) { it }
        for ((ch, child) in root.children) {
            searchRecursive(child, ch, target, currentRow, StringBuilder(), results, maxDistance = 2)
        }

        // Sort by distance ascending, then frequency descending
        val sorted = results
            .distinctBy { it.first }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }.thenByDescending { getWordFrequency(it.first) })
            .map { it.first }
            .take(maxResults)

        // Preserve original capitalization if titlecased
        if (word.isNotEmpty() && word[0].isUpperCase()) {
            return sorted.map { it.replaceFirstChar { c -> c.uppercase() } }
        }
        return sorted
    }

    private fun getWordFrequency(word: String): Int {
        var current = root
        for (ch in word) {
            current = current.children[ch] ?: return 0
        }
        return if (current.isEndOfWord) current.frequency else 0
    }

    private fun searchRecursive(
        node: TrieNode,
        letter: Char,
        target: String,
        previousRow: IntArray,
        currentWord: StringBuilder,
        results: MutableList<Pair<String, Int>>,
        maxDistance: Int
    ) {
        currentWord.append(letter)
        val columns = target.length + 1
        val currentRow = IntArray(columns)
        currentRow[0] = previousRow[0] + 1

        for (i in 1 until columns) {
            val insertCost = currentRow[i - 1] + 1
            val deleteCost = previousRow[i] + 1
            val replaceCost = if (target[i - 1] == letter) previousRow[i - 1] else previousRow[i - 1] + 1
            currentRow[i] = minOf(insertCost, deleteCost, replaceCost)
        }

        if (currentRow[target.length] <= maxDistance && node.isEndOfWord) {
            results.add(Pair(currentWord.toString(), currentRow[target.length]))
        }

        val minInRow = currentRow.minOrNull() ?: Int.MAX_VALUE
        if (minInRow <= maxDistance) {
            for ((ch, child) in node.children) {
                searchRecursive(child, ch, target, currentRow, currentWord, results, maxDistance)
            }
        }

        currentWord.deleteCharAt(currentWord.length - 1)
    }

    override suspend fun check(text: String): List<Correction> = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext emptyList()

        val corrections = mutableListOf<Correction>()
        val wordRegex = Regex("""\b([A-Za-z]+(?:'[A-Za-z]+)?)\b""")

        for (match in wordRegex.findAll(text)) {
            val word = match.value
            if (TextUtils.shouldSkipWord(word)) continue
            // Allow single letters only if 'a' or 'i'
            if (word.length == 1 && !word.equals("a", ignoreCase = true) && !word.equals("i", ignoreCase = true)) {
                continue
            }

            if (!isKnownWord(word)) {
                val suggestions = getSuggestions(word, 3)
                val topSuggestion = suggestions.firstOrNull()

                corrections.add(
                    Correction(
                        startOffset = match.range.first,
                        endOffset = match.range.last + 1,
                        originalText = word,
                        suggestedReplacement = topSuggestion,
                        category = CorrectionCategory.SPELLING,
                        explanation = if (topSuggestion != null) "Possible spelling error. Did you mean \"$topSuggestion\"?"
                                      else "Unknown word \"$word\"",
                        severity = CorrectionSeverity.ERROR,
                        confidence = if (topSuggestion != null) 0.85f else 0.6f,
                        ruleId = "SPELL_CHECK"
                    )
                )
            }
        }

        corrections
    }
}
