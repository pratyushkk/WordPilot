package com.wordpilot.assistant.engine.offline.rules

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.engine.offline.model.AnalyzedSentence
import com.wordpilot.assistant.engine.offline.model.GrammarError
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.morphology.MorphologyAnalyzer
import com.wordpilot.assistant.engine.offline.morphology.VerbForm
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer

class VerbFormRule(
    private val morphology: MorphologyAnalyzer
) : Rule {

    override val id = "VERB_FORM_MISUSE"
    override val category = CorrectionCategory.GRAMMAR
    override val severity = CorrectionSeverity.ERROR
    override val confidence = 0.98f
    override val description = "Use correct verb inflections after auxiliary and modal verbs."

    override val positiveExamples = listOf(
        "They have went to the market.",
        "She has wrote three letters.",
        "I didn't saw what happened.",
        "You must to finish your homework.",
        "He should has called earlier.",
        "We could went together."
    )

    override val negativeExamples = listOf(
        "They have gone to the market.",
        "She has written three letters.",
        "I didn't see what happened.",
        "You must finish your homework.",
        "He should have called earlier.",
        "We could go together."
    )

    override fun isResponsibleFor(errorRuleId: String): Boolean =
        super.isResponsibleFor(errorRuleId) || errorRuleId == "MODAL_INFINITIVE_TO"

    override fun evaluate(sentence: AnalyzedSentence): List<GrammarError> {
        val errors = mutableListOf<GrammarError>()
        val words = sentence.wordTokens
        if (words.size < 2) return errors

        for (i in 0 until words.size - 1) {
            val curr = words[i]
            val next = words[i + 1]

            // 1. Auxiliary HAVE + past tense (e.g. "have went" -> "have gone", "has wrote" -> "has written")
            if (curr.cleanText in setOf("have", "has", "had", "'ve", "'d")) {
                val forms = morphology.analyzeVerbForm(next.cleanText)
                val isPastTenseOnly = forms.any { it.form == VerbForm.PAST } && forms.none { it.form == VerbForm.PAST_PARTICIPLE }
                if (isPastTenseOnly) {
                    val lemma = forms.first { it.form == VerbForm.PAST }.lemma
                    val correctParticiple = morphology.getPastParticiple(lemma)
                    if (correctParticiple != next.cleanText) {
                        errors.add(
                            createError(
                                next,
                                correctParticiple,
                                "Use past participle \"$correctParticiple\" rather than past tense \"${next.text}\" after auxiliary \"${curr.text}\"."
                            )
                        )
                        continue
                    }
                }
            }

            // 2. Auxiliary DID / DIDN'T + past tense (e.g. "did went" -> "did go", "didn't saw" -> "didn't see")
            if (curr.cleanText in setOf("did", "didn't", "didnt")) {
                val forms = morphology.analyzeVerbForm(next.cleanText)
                val isPastTense = forms.any { it.form == VerbForm.PAST }
                if (isPastTense) {
                    val lemma = forms.first { it.form == VerbForm.PAST }.lemma
                    errors.add(
                        createError(
                            next,
                            lemma,
                            "Use base verb form \"$lemma\" after auxiliary \"${curr.text}\"."
                        )
                    )
                    continue
                } else if (next.cleanText.endsWith("ed") && next.cleanText.length > 3) {
                    // Regular past verb e.g. "didn't called" -> "didn't call"
                    val base = morphology.getBaseVerb(next.cleanText)
                    if (base != next.cleanText) {
                        errors.add(
                            createError(
                                next,
                                base,
                                "Use base verb form \"$base\" after auxiliary \"${curr.text}\"."
                            )
                        )
                        continue
                    }
                }
            }

            // 3. Modal verbs (can, could, will, would, should, may, might, must)
            if (curr.cleanText in setOf("can", "could", "will", "would", "shall", "should", "may", "might", "must")) {
                // Modal + "to" + verb: "must to go" -> "must go"
                if (next.cleanText == "to" && i + 2 < words.size) {
                    val verbAfterTo = words[i + 2]
                    errors.add(
                        GrammarError(
                            ruleId = "MODAL_INFINITIVE_TO",
                            startOffset = next.startOffset,
                            endOffset = next.endOffset,
                            originalStartOffset = next.originalStartOffset,
                            originalEndOffset = next.originalEndOffset,
                            originalText = next.text,
                            suggestedReplacement = "",
                            category = category,
                            severity = severity,
                            confidence = confidence,
                            explanation = "Modal verbs like \"${curr.text}\" are followed by bare infinitives without \"to\"."
                        )
                    )
                    continue
                }

                // Modal + "has" / "does" / 3sg form: "should has" -> "should have"
                if (next.cleanText == "has") {
                    errors.add(createError(next, "have", "Modal verbs like \"${curr.text}\" take base verb \"have\"."))
                    continue
                }
                if (next.cleanText == "does") {
                    errors.add(createError(next, "do", "Modal verbs like \"${curr.text}\" take base verb \"do\"."))
                    continue
                }

                // Modal + past tense: "could went" -> "could go"
                val forms = morphology.analyzeVerbForm(next.cleanText)
                val isPastTense = forms.any { it.form == VerbForm.PAST }
                if (isPastTense) {
                    val lemma = forms.first { it.form == VerbForm.PAST }.lemma
                    errors.add(
                        createError(
                            next,
                            lemma,
                            "Use base verb form \"$lemma\" after modal \"${curr.text}\"."
                        )
                    )
                    continue
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
