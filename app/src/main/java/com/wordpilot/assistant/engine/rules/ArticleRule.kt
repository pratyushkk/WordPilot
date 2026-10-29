package com.wordpilot.assistant.engine.rules

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.SentenceSpan
import com.wordpilot.assistant.utils.TextUtils

class ArticleRule : GrammarRule {
    override val id = "ARTICLE_A_AN"
    override val category = CorrectionCategory.GRAMMAR
    override val description = "Detects incorrect use of 'a' vs 'an'"

    // "a" before vowel sound
    private val aBeforeVowel = Regex("""\ba\s+([a-zA-Z]\w*)""", RegexOption.IGNORE_CASE)
    // "an" before consonant sound
    private val anBeforeConsonant = Regex("""\ban\s+([a-zA-Z]\w*)""", RegexOption.IGNORE_CASE)

    override fun check(text: String, sentences: List<SentenceSpan>): List<Correction> {
        val corrections = mutableListOf<Correction>()

        // Check "a" + vowel-sound word
        for (match in aBeforeVowel.findAll(text)) {
            val nextWord = match.groupValues[1]
            if (TextUtils.startsWithVowelSound(nextWord)) {
                val originalArticle = match.value.substring(0, match.value.indexOf(' '))
                val replacement = if (originalArticle[0].isUpperCase()) "An" else "an"
                corrections.add(
                    Correction(
                        startOffset = match.range.first,
                        endOffset = match.range.first + originalArticle.length,
                        originalText = originalArticle,
                        suggestedReplacement = replacement,
                        category = category,
                        explanation = "Use \"$replacement\" before \"$nextWord\" (vowel sound)",
                        severity = CorrectionSeverity.ERROR,
                        confidence = 0.9f,
                        ruleId = id
                    )
                )
            }
        }

        // Check "an" + consonant-sound word
        for (match in anBeforeConsonant.findAll(text)) {
            val nextWord = match.groupValues[1]
            if (!TextUtils.startsWithVowelSound(nextWord)) {
                val originalArticle = match.value.substring(0, match.value.indexOf(' '))
                val replacement = if (originalArticle[0].isUpperCase()) "A" else "a"
                corrections.add(
                    Correction(
                        startOffset = match.range.first,
                        endOffset = match.range.first + originalArticle.length,
                        originalText = originalArticle,
                        suggestedReplacement = replacement,
                        category = category,
                        explanation = "Use \"$replacement\" before \"$nextWord\" (consonant sound)",
                        severity = CorrectionSeverity.ERROR,
                        confidence = 0.9f,
                        ruleId = id
                    )
                )
            }
        }

        return corrections
    }
}
