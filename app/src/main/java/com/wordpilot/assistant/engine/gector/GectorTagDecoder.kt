package com.wordpilot.assistant.engine.gector

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import java.util.UUID

data class TokenSpan(
    val token: String,
    val startOffset: Int,
    val endOffset: Int
)

class GectorTagDecoder {

    companion object {
        const val TAG_KEEP = "\$KEEP"
        const val TAG_DELETE = "\$DELETE"
        const val PREFIX_REPLACE = "\$REPLACE_"
        const val PREFIX_APPEND = "\$APPEND_"
        const val TAG_TRANSFORM_CAPITAL = "\$TRANSFORM_CASE_CAPITAL"
        const val TAG_TRANSFORM_LOWER = "\$TRANSFORM_CASE_LOWER"
    }

    fun decodeTags(
        tokens: List<TokenSpan>,
        predictedTags: List<String>,
        rawText: String
    ): List<Correction> {
        val corrections = mutableListOf<Correction>()
        val count = minOf(tokens.size, predictedTags.size)

        for (i in 0 until count) {
            val tokenSpan = tokens[i]
            val tag = predictedTags[i]

            if (tag == TAG_KEEP || tag.isBlank()) {
                continue
            }

            when {
                tag == TAG_DELETE -> {
                    // Include any following space if present in raw text
                    val endWithSpace = if (tokenSpan.endOffset < rawText.length && rawText[tokenSpan.endOffset] == ' ') {
                        tokenSpan.endOffset + 1
                    } else {
                        tokenSpan.endOffset
                    }
                    val original = rawText.substring(tokenSpan.startOffset, endWithSpace)
                    corrections.add(
                        Correction(
                            id = UUID.randomUUID().toString(),
                            startOffset = tokenSpan.startOffset,
                            endOffset = endWithSpace,
                            originalText = original,
                            suggestedReplacement = "",
                            category = CorrectionCategory.GRAMMAR,
                            explanation = "Extraneous word \"${tokenSpan.token}\"",
                            severity = CorrectionSeverity.WARNING,
                            ruleId = "GECTOR_DELETE",
                            isSafeToApply = true
                        )
                    )
                }

                tag.startsWith(PREFIX_REPLACE) -> {
                    val replacement = tag.removePrefix(PREFIX_REPLACE)
                    val original = rawText.substring(tokenSpan.startOffset, tokenSpan.endOffset)
                    corrections.add(
                        Correction(
                            id = UUID.randomUUID().toString(),
                            startOffset = tokenSpan.startOffset,
                            endOffset = tokenSpan.endOffset,
                            originalText = original,
                            suggestedReplacement = replacement,
                            category = CorrectionCategory.GRAMMAR,
                            explanation = "Replace \"$original\" with \"$replacement\"",
                            severity = CorrectionSeverity.ERROR,
                            ruleId = "GECTOR_REPLACE",
                            isSafeToApply = true
                        )
                    )
                }

                tag.startsWith(PREFIX_APPEND) -> {
                    val appendedWord = tag.removePrefix(PREFIX_APPEND)
                    corrections.add(
                        Correction(
                            id = UUID.randomUUID().toString(),
                            startOffset = tokenSpan.endOffset,
                            endOffset = tokenSpan.endOffset,
                            originalText = "",
                            suggestedReplacement = " $appendedWord",
                            category = CorrectionCategory.GRAMMAR,
                            explanation = "Missing word \"$appendedWord\"",
                            severity = CorrectionSeverity.SUGGESTION,
                            ruleId = "GECTOR_APPEND",
                            isSafeToApply = true
                        )
                    )
                }

                tag == TAG_TRANSFORM_CAPITAL -> {
                    val original = rawText.substring(tokenSpan.startOffset, tokenSpan.endOffset)
                    val capitalized = original.replaceFirstChar { it.uppercaseChar() }
                    corrections.add(
                        Correction(
                            id = UUID.randomUUID().toString(),
                            startOffset = tokenSpan.startOffset,
                            endOffset = tokenSpan.endOffset,
                            originalText = original,
                            suggestedReplacement = capitalized,
                            category = CorrectionCategory.CAPITALIZATION,
                            explanation = "Capitalize \"$original\"",
                            severity = CorrectionSeverity.WARNING,
                            ruleId = "GECTOR_CAPITAL",
                            isSafeToApply = true
                        )
                    )
                }

                tag == TAG_TRANSFORM_LOWER -> {
                    val original = rawText.substring(tokenSpan.startOffset, tokenSpan.endOffset)
                    val lower = original.lowercase()
                    corrections.add(
                        Correction(
                            id = UUID.randomUUID().toString(),
                            startOffset = tokenSpan.startOffset,
                            endOffset = tokenSpan.endOffset,
                            originalText = original,
                            suggestedReplacement = lower,
                            category = CorrectionCategory.CAPITALIZATION,
                            explanation = "Lowercase \"$original\"",
                            severity = CorrectionSeverity.WARNING,
                            ruleId = "GECTOR_LOWER",
                            isSafeToApply = true
                        )
                    )
                }
            }
        }

        return corrections
    }

    private fun isSpellingReplacement(orig: String, repl: String): Boolean {
        if (orig.equals(repl, ignoreCase = true)) return false
        // Levenshtein distance 1 or 2 is typically spelling
        return kotlin.math.abs(orig.length - repl.length) <= 2
    }
}
