package com.wordpilot.assistant.engine.offline.spelling

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.dictionary.EnglishDictionary
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.model.TokenType
import com.wordpilot.assistant.engine.offline.model.WordCasing
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer
import com.wordpilot.assistant.utils.TextUtils

class SpellingChecker(
    private val dictionary: EnglishDictionary
) {

    // Common compound/split word errors: concatenated word -> split replacement
    private val splitWordMap = mapOf(
        "alot" to "a lot",
        "infront" to "in front",
        "everytime" to "every time",
        "nevermind" to "never mind",
        "aswell" to "as well",
        "thankyou" to "thank you",
        "eachother" to "each other",
        "atleast" to "at least",
        "allright" to "all right",
        "nobodys" to "nobody's",
        "anyways" to "anyway",
        "bestfriend" to "best friend",
        "highschool" to "high school",
        "incase" to "in case"
    )

    // Common specific typos with unambiguous replacements
    private val commonTypoMap = mapOf(
        "teh" to "the",
        "recieve" to "receive",
        "recieved" to "received",
        "recieving" to "receiving",
        "beleive" to "believe",
        "beleived" to "believed",
        "seperate" to "separate",
        "seperated" to "separated",
        "definately" to "definitely",
        "definitly" to "definitely",
        "occured" to "occurred",
        "occurance" to "occurrence",
        "accomodate" to "accommodate",
        "embarass" to "embarrass",
        "embarassed" to "embarrassed",
        "tommorrow" to "tomorrow",
        "untill" to "until",
        "truely" to "truly",
        "neccessary" to "necessary",
        "succesful" to "successful",
        "wierd" to "weird",
        "goverment" to "government",
        "enviroment" to "environment",
        "pronounciation" to "pronunciation",
        "calender" to "calendar",
        "priviledge" to "privilege",
        "noticable" to "noticeable"
    )

    fun checkTokens(tokens: List<Token>): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()

        for (token in tokens) {
            if (token.type != TokenType.WORD && token.type != TokenType.CONTRACTION) continue
            val word = token.text

            // Skip heuristics
            if (TextUtils.shouldSkipWord(word)) continue
            if (word.length <= 1 && !word.equals("a", ignoreCase = true) && !word.equals("i", ignoreCase = true)) continue
            if (token.casing == WordCasing.UPPERCASE && word.length in 2..5) continue // Acronyms

            val lower = word.lowercase()

            // 1. Check split words
            val splitReplacement = splitWordMap[lower]
            if (splitReplacement != null) {
                val formatted = TextNormalizer.applyCasing(splitReplacement, token.casing)
                errors.add(
                    GrammarError(
                        ruleId = "SPLIT_WORD",
                        startOffset = token.startOffset,
                        endOffset = token.endOffset,
                        originalStartOffset = token.originalStartOffset,
                        originalEndOffset = token.originalEndOffset,
                        originalText = word,
                        suggestedReplacement = formatted,
                        category = CorrectionCategory.SPELLING,
                        severity = CorrectionSeverity.ERROR,
                        confidence = 0.95f,
                        explanation = "\"$word\" should be written as two words: \"$formatted\""
                    )
                )
                continue
            }

            // 2. Check common known typos
            val typoReplacement = commonTypoMap[lower]
            if (typoReplacement != null) {
                val formatted = TextNormalizer.applyCasing(typoReplacement, token.casing)
                errors.add(
                    GrammarError(
                        ruleId = "SPELL_COMMON_TYPO",
                        startOffset = token.startOffset,
                        endOffset = token.endOffset,
                        originalStartOffset = token.originalStartOffset,
                        originalEndOffset = token.originalEndOffset,
                        originalText = word,
                        suggestedReplacement = formatted,
                        category = CorrectionCategory.SPELLING,
                        severity = CorrectionSeverity.ERROR,
                        confidence = 0.98f,
                        explanation = "Possible spelling error. Did you mean \"$formatted\"?"
                    )
                )
                continue
            }

            // 3. Check general dictionary
            if (!dictionary.isKnownWord(word)) {
                val suggestions = getSuggestions(word, maxResults = 3)
                val top = suggestions.firstOrNull()

                errors.add(
                    GrammarError(
                        ruleId = "SPELL_CHECK",
                        startOffset = token.startOffset,
                        endOffset = token.endOffset,
                        originalStartOffset = token.originalStartOffset,
                        originalEndOffset = token.originalEndOffset,
                        originalText = word,
                        suggestedReplacement = top,
                        category = CorrectionCategory.SPELLING,
                        severity = CorrectionSeverity.ERROR,
                        confidence = if (top != null) 0.88f else 0.60f,
                        explanation = if (top != null) "Possible spelling error. Did you mean \"$top\"?" else "Unknown word \"$word\""
                    )
                )
            }
        }

        return errors
    }

    fun getSuggestions(word: String, maxResults: Int = 5): List<String> {
        val target = word.trim().lowercase()
        if (target.isEmpty()) return emptyList()

        // 1. Fast check for common typo map
        val instant = commonTypoMap[target]
        if (instant != null) {
            val formatted = TextNormalizer.applyCasing(instant, TextNormalizer.detectCasing(word))
            return listOf(formatted)
        }

        // 2. Trie-based Damerau-Levenshtein search (maxDistance = 2)
        val candidates = mutableListOf<Candidate>()
        val currentRow = IntArray(target.length + 1) { it }

        for ((ch, child) in dictionary.root.children) {
            searchTrie(child, ch, target, currentRow, StringBuilder(), candidates, maxDistance = 2, parentChar = null)
        }

        val sorted = candidates
            .distinctBy { it.word }
            .sortedWith(
                compareBy<Candidate> { it.distance }
                    .thenByDescending { it.frequency }
            )
            .map { it.word }
            .take(maxResults)

        val casing = TextNormalizer.detectCasing(word)
        return sorted.map { TextNormalizer.applyCasing(it, casing) }
    }

    private data class Candidate(val word: String, val distance: Int, val frequency: Int)

    private fun searchTrie(
        node: EnglishDictionary.TrieNode,
        letter: Char,
        target: String,
        previousRow: IntArray,
        currentWord: StringBuilder,
        results: MutableList<Candidate>,
        maxDistance: Int,
        parentChar: Char?
    ) {
        currentWord.append(letter)
        val columns = target.length + 1
        val currentRow = IntArray(columns)
        currentRow[0] = previousRow[0] + 1

        for (i in 1 until columns) {
            val targetChar = target[i - 1]
            val cost = if (targetChar == letter) 0 else 1

            val insertCost = currentRow[i - 1] + 1
            val deleteCost = previousRow[i] + 1
            val replaceCost = previousRow[i - 1] + cost

            var minCost = minOf(insertCost, deleteCost, replaceCost)

            // Damerau transposition check
            if (i > 1 && parentChar != null && target[i - 1] == parentChar && target[i - 2] == letter) {
                // Approximate transposition cost bonus
                minCost = minOf(minCost, previousRow[i - 2] + 1)
            }

            currentRow[i] = minCost
        }

        if (currentRow[target.length] <= maxDistance && node.isWord) {
            results.add(Candidate(currentWord.toString(), currentRow[target.length], node.frequency))
        }

        val minInRow = currentRow.minOrNull() ?: Int.MAX_VALUE
        if (minInRow <= maxDistance) {
            for ((ch, child) in node.children) {
                searchTrie(child, ch, target, currentRow, currentWord, results, maxDistance, letter)
            }
        }

        currentWord.deleteCharAt(currentWord.length - 1)
    }
}
