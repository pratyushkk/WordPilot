package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.model.WordCasing
import com.wordpilot.assistant.engine.offline.morphology.MorphologyAnalyzer
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class SubjectVerbAgreementRule(
    private val morphology: MorphologyAnalyzer
) : Rule {

    override val id = "SUBJECT_VERB_AGREEMENT"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.ERROR
    override val confidence = 0.95f
    override val description = "Subject and verb must agree in number (singular with singular, plural with plural)."

    override val positiveExamples = listOf(
        "She have a new car.",
        "They has arrived safely.",
        "The dog bark loudly at strangers.",
        "The dogs barks in the yard.",
        "There is many reasons to stay.",
        "Everyone are invited to the party."
    )

    override val negativeExamples = listOf(
        "She has a new car.",
        "They have arrived safely.",
        "The dog barks loudly at strangers.",
        "The dogs bark in the yard.",
        "There are many reasons to stay.",
        "Everyone is invited to the party."
    )

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.size < 2) return errors

        for (i in 0 until words.size - 1) {
            val curr = words[i]
            val next = words[i + 1]

            // Pattern 1: Singular pronoun + plural/base verb
            // e.g. "he go", "she have", "it do", "he don't"
            if (curr.cleanText in setOf("he", "she", "it")) {
                if (next.cleanText == "go") {
                    errors.add(createError(next, "goes", "Use \"goes\" with third-person singular pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.cleanText == "have") {
                    errors.add(createError(next, "has", "Use \"has\" with third-person singular pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.cleanText == "do") {
                    errors.add(createError(next, "does", "Use \"does\" with third-person singular pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.cleanText == "don't") {
                    errors.add(createError(next, "doesn't", "Use \"doesn't\" with third-person singular pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.cleanText == "were") {
                    errors.add(createError(next, "was", "Use \"was\" with third-person singular pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.tag == POSTag.VB || next.tag == POSTag.VBP || next.cleanText in setOf("run", "see", "come", "say", "take", "make", "know", "think", "give", "tell", "bark", "walk", "jump", "play")) {
                    val sVerb = morphology.getThirdPersonSingularVerb(next.cleanText)
                    if (sVerb != next.cleanText && next.cleanText !in setOf("can", "could", "will", "would", "shall", "should", "may", "might", "must")) {
                        errors.add(createError(next, sVerb, "Use singular verb \"$sVerb\" with third-person singular pronoun \"${curr.text}\"."))
                        continue
                    }
                }
            }

            // Pattern 2: Plural pronoun + singular verb
            // e.g. "they has", "we was", "they goes", "you is", "we does", "they doesn't"
            if (curr.cleanText in setOf("they", "we", "you")) {
                if (next.cleanText == "has") {
                    errors.add(createError(next, "have", "Use \"have\" with plural pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.cleanText == "was") {
                    errors.add(createError(next, "were", "Use \"were\" with plural pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.cleanText == "is") {
                    errors.add(createError(next, "are", "Use \"are\" with plural pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.cleanText == "does") {
                    errors.add(createError(next, "do", "Use \"do\" with plural pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.cleanText == "doesn't") {
                    errors.add(createError(next, "don't", "Use \"don't\" with plural pronoun \"${curr.text}\"."))
                    continue
                }
                if (next.tag == POSTag.VBZ && next.cleanText != "has" && next.cleanText != "is" && next.cleanText != "does") {
                    val baseVerb = morphology.getBaseVerb(next.cleanText)
                    if (baseVerb != next.cleanText) {
                        errors.add(createError(next, baseVerb, "Use base verb \"$baseVerb\" with plural pronoun \"${curr.text}\"."))
                        continue
                    }
                }
            }

            // Pattern 3: "I" agreement
            if (curr.cleanText == "i") {
                if (next.cleanText == "has") {
                    errors.add(createError(next, "have", "Use \"have\" with \"I\"."))
                    continue
                }
                if (next.cleanText == "is" || next.cleanText == "are") {
                    errors.add(createError(next, "am", "Use \"am\" with \"I\"."))
                    continue
                }
                if (next.cleanText == "does") {
                    errors.add(createError(next, "do", "Use \"do\" with \"I\"."))
                    continue
                }
                if (next.cleanText == "doesn't") {
                    errors.add(createError(next, "don't", "Use \"don't\" with \"I\"."))
                    continue
                }
                if (next.tag == POSTag.VBZ) {
                    val baseVerb = morphology.getBaseVerb(next.cleanText)
                    errors.add(createError(next, baseVerb, "Use base verb \"$baseVerb\" with \"I\"."))
                    continue
                }
            }

            // Pattern 4: Indefinite pronouns (singular)
            // e.g. "everyone are" -> "is", "each have" -> "has", "everybody know" -> "knows"
            if (curr.cleanText in setOf("everyone", "everybody", "someone", "somebody", "anyone", "anybody", "nobody", "no one", "each")) {
                if (next.cleanText == "are") {
                    errors.add(createError(next, "is", "Indefinite pronouns such as \"${curr.text}\" take a singular verb."))
                    continue
                }
                if (next.cleanText == "have") {
                    errors.add(createError(next, "has", "Indefinite pronouns such as \"${curr.text}\" take a singular verb."))
                    continue
                }
                if (next.cleanText == "were") {
                    errors.add(createError(next, "was", "Indefinite pronouns such as \"${curr.text}\" take a singular verb."))
                    continue
                }
            }

            // Pattern 5: Existential "There is" / "There are"
            // "There is many / two / cats..."
            if (curr.cleanText == "there" && i + 2 < words.size) {
                val verb = words[i + 1]
                val obj = words[i + 2]

                if (verb.cleanText == "is" || verb.cleanText == "was") {
                    if (obj.cleanText in setOf("many", "several", "few", "two", "three", "four", "five") || obj.tag == POSTag.NNS || morphology.isPluralNoun(obj.cleanText)) {
                        val rep = if (verb.cleanText == "is") "are" else "were"
                        errors.add(createError(verb, rep, "Use plural verb \"$rep\" with plural subject \"${obj.text}\"."))
                        continue
                    }
                } else if (verb.cleanText == "are" || verb.cleanText == "were") {
                    if (obj.cleanText in setOf("a", "an", "one", "this", "that") || (obj.tag == POSTag.NN && !morphology.isPluralNoun(obj.cleanText))) {
                        val rep = if (verb.cleanText == "are") "is" else "was"
                        errors.add(createError(verb, rep, "Use singular verb \"$rep\" with singular subject \"${obj.text}\"."))
                        continue
                    }
                }
            }

            // Pattern 6: Noun + Verb agreement
            // Singular noun (not mass noun, preceded by DT "the/this/that") + plural/base verb:
            // e.g. "The dog bark" -> "The dog barks"
            if (i > 0 && words[i - 1].cleanText in setOf("the", "this", "that", "my", "your", "his", "her", "our", "their")) {
                val isSingularNoun = curr.tag == POSTag.NN && !morphology.isPluralNoun(curr.cleanText) && !morphology.isUncountable(curr.cleanText)
                val isPluralNoun = curr.tag == POSTag.NNS || morphology.isPluralNoun(curr.cleanText)

                if (isSingularNoun) {
                    if (next.cleanText == "have") {
                        errors.add(createError(next, "has", "Singular noun \"${curr.text}\" takes singular verb \"has\"."))
                    } else if (next.cleanText == "were") {
                        errors.add(createError(next, "was", "Singular noun \"${curr.text}\" takes singular verb \"was\"."))
                    } else if (next.cleanText == "don't") {
                        errors.add(createError(next, "doesn't", "Singular noun \"${curr.text}\" takes singular verb \"doesn't\"."))
                    } else if (next.tag == POSTag.VB || next.tag == POSTag.VBP || next.cleanText in setOf("bark", "run", "jump", "walk", "sing", "play", "bite")) {
                        val sVerb = morphology.getThirdPersonSingularVerb(next.cleanText)
                        if (sVerb != next.cleanText && sVerb.endsWith("s") && next.cleanText !in setOf("can", "could", "will", "would", "must", "should", "may", "might")) {
                            errors.add(createError(next, sVerb, "Use singular verb \"$sVerb\" with singular subject \"${curr.text}\"."))
                        }
                    }
                } else if (isPluralNoun) {
                    if (next.cleanText == "has") {
                        errors.add(createError(next, "have", "Plural noun \"${curr.text}\" takes plural verb \"have\"."))
                    } else if (next.cleanText == "was") {
                        errors.add(createError(next, "were", "Plural noun \"${curr.text}\" takes plural verb \"were\"."))
                    } else if (next.cleanText == "is") {
                        errors.add(createError(next, "are", "Plural noun \"${curr.text}\" takes plural verb \"are\"."))
                    } else if (next.cleanText == "doesn't") {
                        errors.add(createError(next, "don't", "Plural noun \"${curr.text}\" takes plural verb \"don't\"."))
                    } else if ((next.tag == POSTag.VBZ || next.cleanText in setOf("barks", "runs", "jumps", "walks", "sings", "plays", "bites")) && next.cleanText != "is" && next.cleanText != "has" && next.cleanText != "does") {
                        val baseVerb = morphology.getBaseVerb(next.cleanText)
                        if (baseVerb != next.cleanText) {
                            errors.add(createError(next, baseVerb, "Use plural verb \"$baseVerb\" with plural subject \"${curr.text}\"."))
                        }
                    }
                }
            }
        }

        return errors
    }

    private fun createError(token: Token, replacement: String, exp: String): GrammarError {
        val formatted = TextNormalizer.applyCasing(replacement, token.casing)
        return GrammarError(
            ruleId = id,
            startOffset = token.startOffset,
            endOffset = token.endOffset,
            originalStartOffset = token.originalStartOffset,
            originalEndOffset = token.originalEndOffset,
            originalText = token.text,
            suggestedReplacement = formatted,
            category = category,
            severity = severity,
            confidence = confidence,
            explanation = exp
        )
    }
}
