package com.wordpilot.assistant.engine.offline.tokenizer

import com.wordpilot.assistant.engine.offline.model.NormalizedResult
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.model.TokenType
import com.wordpilot.assistant.engine.offline.model.WordCasing
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class Tokenizer {

    fun tokenize(text: String, normResult: NormalizedResult? = null, baseOffset: Int = 0): List<Token> {
        if (text.isEmpty()) return emptyList()

        val tokens = mutableListOf<Token>()
        val len = text.length
        var i = 0

        while (i < len) {
            val ch = text[i]

            // 1. Whitespace
            if (ch.isWhitespace()) {
                val start = i
                while (i < len && text[i].isWhitespace()) {
                    i++
                }
                tokens.add(createToken(text, start, i, TokenType.WHITESPACE, normResult, baseOffset))
                continue
            }

            // 2. URLs & Emails
            if (ch == 'h' && text.startsWith("http://", i) || text.startsWith("https://", i)) {
                val start = i
                while (i < len && !text[i].isWhitespace()) {
                    i++
                }
                tokens.add(createToken(text, start, i, TokenType.URL, normResult, baseOffset))
                continue
            }

            // 3. Numbers (including decimals like 3.14 or currency $10)
            if (ch.isDigit()) {
                val start = i
                while (i < len && (text[i].isDigit() || (text[i] == '.' && i + 1 < len && text[i + 1].isDigit()) || (text[i] == ',' && i + 1 < len && text[i + 1].isDigit()))) {
                    i++
                }
                tokens.add(createToken(text, start, i, TokenType.NUMBER, normResult, baseOffset))
                continue
            }

            // 4. Words & Contractions
            if (ch.isLetter()) {
                val start = i
                while (i < len) {
                    val c = text[i]
                    if (c.isLetter()) {
                        i++
                    } else if (c == '\'' && i + 1 < len && text[i + 1].isLetter()) {
                        // Embedded apostrophe: contraction or possessive e.g. "don't", "cat's"
                        i++
                    } else if (c == '-' && i + 1 < len && text[i + 1].isLetter() && (i - start) > 1) {
                        // Hyphenated compound word: "well-known", "subject-verb"
                        i++
                    } else {
                        break
                    }
                }
                val wordStr = text.substring(start, i)
                val type = if (wordStr.contains('\'')) TokenType.CONTRACTION else TokenType.WORD
                tokens.add(createToken(text, start, i, type, normResult, baseOffset))
                continue
            }

            // 5. Punctuation
            if (ch in setOf('.', ',', '!', '?', ';', ':', '-', '—', '"', '\'', '(', ')', '[', ']', '{', '}')) {
                val start = i
                // Check for multi-char punctuation like "..." or "--" or "?!"
                if (ch == '.' && i + 2 < len && text[i + 1] == '.' && text[i + 2] == '.') {
                    i += 3
                } else if (ch == '-' && i + 1 < len && text[i + 1] == '-') {
                    i += 2
                } else {
                    i++
                }
                tokens.add(createToken(text, start, i, TokenType.PUNCTUATION, normResult, baseOffset))
                continue
            }

            // 6. Other Symbols (@, #, $, %, etc.)
            val start = i
            i++
            tokens.add(createToken(text, start, i, TokenType.SYMBOL, normResult, baseOffset))
        }

        return tokens
    }

    private fun createToken(
        text: String,
        start: Int,
        end: Int,
        type: TokenType,
        normResult: NormalizedResult?,
        baseOffset: Int
    ): Token {
        val rawToken = text.substring(start, end)
        val absStart = baseOffset + start
        val absEnd = baseOffset + end

        val origStart = normResult?.toOriginalOffset(absStart) ?: absStart
        val origEnd = normResult?.toOriginalOffset(absEnd) ?: absEnd

        val clean = rawToken.lowercase()
        val casing = if (type == TokenType.WORD || type == TokenType.CONTRACTION) {
            TextNormalizer.detectCasing(rawToken)
        } else {
            WordCasing.LOWERCASE
        }

        return Token(
            text = rawToken,
            cleanText = clean,
            startOffset = absStart,
            endOffset = absEnd,
            originalStartOffset = origStart,
            originalEndOffset = origEnd,
            type = type,
            casing = casing
        )
    }
}
