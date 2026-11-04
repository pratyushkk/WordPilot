package com.wordpilot.assistant.engine.offline.segmenter

import com.wordpilot.assistant.engine.offline.model.NormalizedResult
import com.wordpilot.assistant.engine.offline.model.SentenceSpan

class SentenceSegmenter {

    private val commonAbbreviations = hashSetOf(
        "mr.", "mrs.", "ms.", "dr.", "prof.", "sr.", "jr.", "rev.", "gen.", "col.", "capt.", "sgt.",
        "e.g.", "i.e.", "etc.", "vs.", "v.", "cf.", "al.", "ibid.", "ca.",
        "inc.", "ltd.", "corp.", "co.", "dept.",
        "st.", "ave.", "blvd.", "rd.", "ln.", "apt.",
        "a.m.", "p.m.", "no.", "nos.", "vol.", "vols.", "pp.", "p.", "fig.", "eq.",
        "approx.", "est.", "misc."
    )

    fun segment(normalizedText: String, normResult: NormalizedResult? = null): List<SentenceSpan> {
        if (normalizedText.isBlank()) return emptyList()

        val spans = mutableListOf<SentenceSpan>()
        val len = normalizedText.length
        var sentenceStart = 0

        var idx = 0
        while (idx < len) {
            val ch = normalizedText[idx]

            if (ch == '.' || ch == '!' || ch == '?') {
                // Check if this punctuation mark actually ends a sentence
                if (isSentenceBoundary(normalizedText, idx)) {
                    // Consume closing quotes or brackets attached to the punctuation
                    var boundaryEnd = idx + 1
                    while (boundaryEnd < len && (normalizedText[boundaryEnd] in setOf('"', '\'', ')', ']', '}'))) {
                        boundaryEnd++
                    }

                    val sentText = normalizedText.substring(sentenceStart, boundaryEnd).trim()
                    if (sentText.isNotEmpty()) {
                        val trimmedStart = sentenceStart + (normalizedText.substring(sentenceStart, boundaryEnd).indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0))
                        val trimmedEnd = sentenceStart + (normalizedText.substring(sentenceStart, boundaryEnd).indexOfLast { !it.isWhitespace() }.coerceAtLeast(0) + 1)

                        val origStart = normResult?.toOriginalOffset(trimmedStart) ?: trimmedStart
                        val origEnd = normResult?.toOriginalOffset(trimmedEnd) ?: trimmedEnd

                        spans.add(
                            SentenceSpan(
                                start = trimmedStart,
                                end = trimmedEnd,
                                text = sentText,
                                originalStart = origStart,
                                originalEnd = origEnd
                            )
                        )
                    }

                    // Move sentenceStart past whitespace
                    sentenceStart = boundaryEnd
                    while (sentenceStart < len && normalizedText[sentenceStart].isWhitespace()) {
                        sentenceStart++
                    }
                    idx = sentenceStart
                    continue
                }
            }
            idx++
        }

        // Remaining text after last punctuation
        if (sentenceStart < len) {
            val remaining = normalizedText.substring(sentenceStart).trim()
            if (remaining.isNotEmpty()) {
                val trimmedStart = sentenceStart + (normalizedText.substring(sentenceStart).indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0))
                val trimmedEnd = sentenceStart + (normalizedText.substring(sentenceStart).indexOfLast { !it.isWhitespace() }.coerceAtLeast(0) + 1)

                val origStart = normResult?.toOriginalOffset(trimmedStart) ?: trimmedStart
                val origEnd = normResult?.toOriginalOffset(trimmedEnd) ?: trimmedEnd

                spans.add(
                    SentenceSpan(
                        start = trimmedStart,
                        end = trimmedEnd,
                        text = remaining,
                        originalStart = origStart,
                        originalEnd = origEnd
                    )
                )
            }
        }

        return spans
    }

    private fun isSentenceBoundary(text: String, dotIndex: Int): Boolean {
        val ch = text[dotIndex]
        val len = text.length

        // For ! and ?, check if followed by whitespace or quote/bracket then uppercase or end of text
        if (ch == '!' || ch == '?') {
            return isTerminalFollowedByNextSentence(text, dotIndex)
        }

        // Decimal number check: 3.14
        if (dotIndex > 0 && dotIndex + 1 < len && text[dotIndex - 1].isDigit() && text[dotIndex + 1].isDigit()) {
            return false
        }

        // Ellipsis check: ...
        if ((dotIndex + 1 < len && text[dotIndex + 1] == '.') || (dotIndex > 0 && text[dotIndex - 1] == '.')) {
            // Check if at the very end of ellipsis
            var endOfEllipsis = dotIndex
            while (endOfEllipsis < len && text[endOfEllipsis] == '.') {
                endOfEllipsis++
            }
            return isTerminalFollowedByNextSentence(text, endOfEllipsis - 1)
        }

        // URL / domain check: e.g. "wordpilot.com"
        if (dotIndex > 0 && dotIndex + 1 < len && !text[dotIndex - 1].isWhitespace() && !text[dotIndex + 1].isWhitespace()) {
            return false
        }

        // Extract preceding word to test against abbreviations
        var wordStart = dotIndex - 1
        while (wordStart >= 0 && (text[wordStart].isLetter() || text[wordStart] == '.')) {
            wordStart--
        }
        val precedingWord = text.substring(wordStart + 1, dotIndex + 1).lowercase()

        if (precedingWord in commonAbbreviations) {
            if (precedingWord in setOf("a.m.", "p.m.", "etc.", "approx.")) {
                return isTerminalFollowedByNextSentence(text, dotIndex)
            }
            return false
        }

        // Single capital letter initial like "J. K. Rowling"
        if (precedingWord.length == 2 && precedingWord[0].isLetter()) {
            return false
        }

        return isTerminalFollowedByNextSentence(text, dotIndex)
    }

    private fun isTerminalFollowedByNextSentence(text: String, punctIdx: Int): Boolean {
        var peek = punctIdx + 1
        // Skip closing quotes or brackets
        while (peek < text.length && text[peek] in setOf('"', '\'', ')', ']', '}')) {
            peek++
        }

        if (peek >= text.length) return true // End of text is a valid boundary

        if (!text[peek].isWhitespace()) {
            return false // Must be followed by whitespace
        }

        // Skip whitespace
        while (peek < text.length && text[peek].isWhitespace()) {
            peek++
        }

        if (peek >= text.length) return true

        // Next character is uppercase letter, quote, or bullet
        val nextChar = text[peek]
        return nextChar.isUpperCase() || nextChar in setOf('"', '\'', '(', '[', '{', '•', '-')
    }
}
