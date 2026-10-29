package com.wordpilot.assistant.engine.rules

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.SentenceSpan

class ConfusedWordsRule : GrammarRule {
    override val id = "CONFUSED_WORDS"
    override val category = CorrectionCategory.GRAMMAR
    override val description = "Detects commonly confused words"

    private data class ConfusedPattern(
        val pattern: Regex,
        val groupIndex: Int,
        val replacement: String,
        val explanation: String,
        val confidence: Float = 0.85f
    )

    private val patterns = listOf(
        // "their" + verb → "they're"
        ConfusedPattern(
            Regex("""\b(their)\s+(going|coming|doing|making|trying|running|getting|being|having|leaving|staying|playing|looking|working|taking|saying|thinking|feeling|walking)\b""", RegexOption.IGNORE_CASE),
            1,
            "they're",
            "\"They're\" (they are) is likely intended here, not \"their\" (possessive)"
        ),
        // "there" + verb → "they're"
        ConfusedPattern(
            Regex("""\b(there)\s+(going|coming|doing|making|trying|running|getting|being|having)\b""", RegexOption.IGNORE_CASE),
            1,
            "they're",
            "\"They're\" (they are) is likely intended here, not \"there\" (location)"
        ),
        // "your" + verb → "you're"
        ConfusedPattern(
            Regex("""\b(your)\s+(going|coming|doing|making|trying|running|getting|being|looking|welcome|right|wrong|sure|welcome)\b""", RegexOption.IGNORE_CASE),
            1,
            "you're",
            "\"You're\" (you are) is likely intended here, not \"your\" (possessive)"
        ),
        // "its" + verb → "it's" (when followed by certain verbs/adjectives)
        ConfusedPattern(
            Regex("""\b(its)\s+(going|coming|been|being|a\s|the\s|not|very|really|quite|pretty|so\s|too\s|just|already|always|never|still|also|about|over|only)\b""", RegexOption.IGNORE_CASE),
            1,
            "it's",
            "\"It's\" (it is/it has) is likely intended here, not \"its\" (possessive)"
        ),
        // "then" after comparisons → "than"
        ConfusedPattern(
            Regex("""\b(better|worse|more|less|greater|fewer|bigger|smaller|larger|taller|shorter|faster|slower|higher|lower|older|younger|longer|rather)\s+(then)\b""", RegexOption.IGNORE_CASE),
            2,
            "than",
            "Use \"than\" for comparisons, not \"then\" (time/sequence)"
        ),
        // "would of" → "would have"
        ConfusedPattern(
            Regex("""\b(would|could|should|might|must)\s+(of)\b""", RegexOption.IGNORE_CASE),
            2,
            "have",
            "Use \"have\" instead of \"of\" after modal verbs"
        ),
        // "alot" → "a lot"
        ConfusedPattern(
            Regex("""\b(alot)\b""", RegexOption.IGNORE_CASE),
            1,
            "a lot",
            "\"A lot\" is two words"
        ),
        // "definately" → "definitely"
        ConfusedPattern(
            Regex("""\b(definately|definatly|definetly|definetely)\b""", RegexOption.IGNORE_CASE),
            1,
            "definitely",
            "Correct spelling is \"definitely\""
        ),
        // "seperate" → "separate"
        ConfusedPattern(
            Regex("""\b(seperate|seprat|separete)\b""", RegexOption.IGNORE_CASE),
            1,
            "separate",
            "Correct spelling is \"separate\""
        ),
        // "recieve" → "receive"
        ConfusedPattern(
            Regex("""\b(recieve|recive)\b""", RegexOption.IGNORE_CASE),
            1,
            "receive",
            "Correct spelling is \"receive\" (i before e except after c)"
        ),
        // "occured" → "occurred"
        ConfusedPattern(
            Regex("""\b(occured)\b""", RegexOption.IGNORE_CASE),
            1,
            "occurred",
            "Correct spelling is \"occurred\" (double r)"
        ),
        // "accomodate" → "accommodate"
        ConfusedPattern(
            Regex("""\b(accomodate|acommodate|acomodate)\b""", RegexOption.IGNORE_CASE),
            1,
            "accommodate",
            "Correct spelling is \"accommodate\" (double c, double m)"
        ),
    )

    override fun check(text: String, sentences: List<SentenceSpan>): List<Correction> {
        val corrections = mutableListOf<Correction>()

        for (cp in patterns) {
            for (match in cp.pattern.findAll(text)) {
                val target = match.groups[cp.groupIndex] ?: continue
                val original = target.value
                
                // Preserve original casing for replacement
                val replacement = if (original[0].isUpperCase()) {
                    cp.replacement.replaceFirstChar { it.uppercase() }
                } else {
                    cp.replacement
                }

                corrections.add(
                    Correction(
                        startOffset = target.range.first,
                        endOffset = target.range.last + 1,
                        originalText = original,
                        suggestedReplacement = replacement,
                        category = category,
                        explanation = cp.explanation,
                        severity = CorrectionSeverity.WARNING,
                        confidence = cp.confidence,
                        ruleId = "$id.${original.uppercase()}"
                    )
                )
            }
        }

        return corrections
    }
}
