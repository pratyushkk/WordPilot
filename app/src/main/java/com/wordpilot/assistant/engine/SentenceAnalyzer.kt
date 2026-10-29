package com.wordpilot.assistant.engine

data class SentenceSpan(
    val start: Int,
    val end: Int,
    val text: String
)

class SentenceAnalyzer {
    private val sentenceEndPattern = Regex("""[.!?]+\s+""")
    private val abbreviations = setOf(
        "mr.", "mrs.", "ms.", "dr.", "prof.", "sr.", "jr.",
        "vs.", "etc.", "inc.", "ltd.", "dept.", "approx.",
        "st.", "ave.", "blvd.", "apt.", "no.", "vol.",
        "fig.", "eq.", "e.g.", "i.e.", "a.m.", "p.m."
    )

    fun splitSentences(text: String): List<SentenceSpan> {
        if (text.isBlank()) return emptyList()

        val sentences = mutableListOf<SentenceSpan>()
        var start = 0
        
        val matches = sentenceEndPattern.findAll(text)
        
        for (match in matches) {
            val potentialEnd = match.range.last + 1
            val beforeMatch = text.substring(start, match.range.first + 1).trimEnd()
            
            // Check if this is an abbreviation
            val lastWord = beforeMatch.split("\\s+".toRegex()).lastOrNull()?.lowercase() ?: ""
            if (lastWord in abbreviations) continue
            
            val sentenceText = text.substring(start, potentialEnd).trim()
            if (sentenceText.isNotEmpty()) {
                sentences.add(SentenceSpan(start, potentialEnd, sentenceText))
            }
            start = potentialEnd
        }
        
        // Add remaining text as last sentence
        if (start < text.length) {
            val remaining = text.substring(start).trim()
            if (remaining.isNotEmpty()) {
                sentences.add(SentenceSpan(start, text.length, remaining))
            }
        }
        
        return sentences
    }

    fun findCurrentSentence(text: String, cursorPosition: Int): SentenceSpan? {
        val sentences = splitSentences(text)
        return sentences.find { cursorPosition in it.start..it.end }
            ?: sentences.lastOrNull()
    }

    fun findWordAtPosition(text: String, position: Int): Pair<Int, Int>? {
        if (position < 0 || position > text.length || text.isEmpty()) return null
        
        var wordStart = position
        while (wordStart > 0 && !text[wordStart - 1].isWhitespace()) {
            wordStart--
        }
        
        var wordEnd = position
        while (wordEnd < text.length && !text[wordEnd].isWhitespace()) {
            wordEnd++
        }
        
        return if (wordStart < wordEnd) Pair(wordStart, wordEnd) else null
    }

    fun isWordComplete(text: String, wordEnd: Int): Boolean {
        if (wordEnd >= text.length) return false
        val nextChar = text[wordEnd]
        return nextChar.isWhitespace() || nextChar in setOf('.', ',', '!', '?', ';', ':', ')', ']', '}', '"', '\'')
    }
}
