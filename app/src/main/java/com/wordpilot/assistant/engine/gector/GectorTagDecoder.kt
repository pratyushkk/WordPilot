package com.wordpilot.assistant.engine.gector

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import java.util.UUID

class GectorTagDecoder(
    private val conjugator: GectorVerbConjugator? = null
) {
    companion object {
        const val TAG_KEEP = "\$KEEP"
        const val TAG_DELETE = "\$DELETE"
        const val PREFIX_REPLACE = "\$REPLACE_"
        const val PREFIX_APPEND = "\$APPEND_"
        const val PREFIX_TRANSFORM_VERB = "\$TRANSFORM_VERB_"
        const val PREFIX_TRANSFORM_AGREEMENT = "\$TRANSFORM_AGREEMENT_"
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

            if (tag == TAG_KEEP || tag.isBlank() || tag == "<OOV>") {
                continue
            }

            // Only decode on the start of a word to avoid applying edits to multiple subwords
            if (!tokenSpan.isWordStart) {
                continue
            }

            if (tokenSpan.startOffset < 0 || tokenSpan.endOffset > rawText.length || tokenSpan.startOffset >= tokenSpan.endOffset) {
                continue
            }

            when {
                tag == TAG_DELETE -> {
                    // Include trailing whitespace if present
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
                    val category = if (isSpellingError(original, replacement)) {
                        CorrectionCategory.SPELLING
                    } else {
                        CorrectionCategory.GRAMMAR
                    }

                    corrections.add(
                        Correction(
                            id = UUID.randomUUID().toString(),
                            startOffset = tokenSpan.startOffset,
                            endOffset = tokenSpan.endOffset,
                            originalText = original,
                            suggestedReplacement = matchCase(original, replacement),
                            category = category,
                            explanation = "Replace \"$original\" with \"$replacement\"",
                            severity = if (category == CorrectionCategory.SPELLING) CorrectionSeverity.ERROR else CorrectionSeverity.WARNING,
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

                tag.startsWith(PREFIX_TRANSFORM_VERB) -> {
                    val rule = tag.removePrefix(PREFIX_TRANSFORM_VERB)
                    val original = rawText.substring(tokenSpan.startOffset, tokenSpan.endOffset)
                    val conjugated = conjugator?.conjugate(original, rule)

                    if (conjugated != null && conjugated != original) {
                        corrections.add(
                            Correction(
                                id = UUID.randomUUID().toString(),
                                startOffset = tokenSpan.startOffset,
                                endOffset = tokenSpan.endOffset,
                                originalText = original,
                                suggestedReplacement = conjugated,
                                category = CorrectionCategory.GRAMMAR,
                                explanation = "Change verb form to \"$conjugated\"",
                                severity = CorrectionSeverity.ERROR,
                                ruleId = "GECTOR_VERB_$rule",
                                isSafeToApply = true
                            )
                        )
                    }
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

                tag.startsWith(PREFIX_TRANSFORM_AGREEMENT) -> {
                    val original = rawText.substring(tokenSpan.startOffset, tokenSpan.endOffset)
                    val agreementType = tag.removePrefix(PREFIX_TRANSFORM_AGREEMENT)
                    val replacement = when (agreementType) {
                        "SINGULAR" -> if (original.endsWith("s", ignoreCase = true)) original.dropLast(1) else null
                        "PLURAL" -> original + "s"
                        else -> null
                    }
                    if (replacement != null) {
                        corrections.add(
                            Correction(
                                id = UUID.randomUUID().toString(),
                                startOffset = tokenSpan.startOffset,
                                endOffset = tokenSpan.endOffset,
                                originalText = original,
                                suggestedReplacement = replacement,
                                category = CorrectionCategory.GRAMMAR,
                                explanation = "Number agreement: change to \"$replacement\"",
                                severity = CorrectionSeverity.WARNING,
                                ruleId = "GECTOR_AGREEMENT",
                                isSafeToApply = true
                            )
                        )
                    }
                }
            }
        }

        return corrections
    }

    private fun matchCase(original: String, replacement: String): String {
        return when {
            original.all { it.isUpperCase() } -> replacement.uppercase()
            original.isNotEmpty() && original[0].isUpperCase() -> replacement.replaceFirstChar { it.uppercaseChar() }
            else -> replacement
        }
    }

    private fun isSpellingError(orig: String, repl: String): Boolean {
        if (orig.equals(repl, ignoreCase = true)) return false
        val dist = levenshtein(orig.lowercase(), repl.lowercase())
        return dist <= 2 && orig.length > 2
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
