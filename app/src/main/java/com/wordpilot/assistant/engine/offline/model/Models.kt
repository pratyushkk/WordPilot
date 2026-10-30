package com.wordpilot.assistant.engine.offline.model

import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import java.util.UUID

enum class TokenType {
    WORD,
    NUMBER,
    PUNCTUATION,
    WHITESPACE,
    CONTRACTION,
    URL,
    EMAIL,
    SYMBOL
}

enum class WordCasing {
    LOWERCASE,
    TITLECASE,
    UPPERCASE,
    MIXED
}

enum class POSTag {
    NN,        // Noun, singular or mass (cat, apple, money)
    NNS,       // Noun, plural (cats, apples)
    NNP,       // Proper noun, singular (London, Google, John)
    NNPS,      // Proper noun, plural (Americans)
    PRP,       // Personal pronoun subject (I, you, he, she, it, we, they)
    PRP_DO,    // Personal pronoun object (me, you, him, her, it, us, them)
    PRP_POS,   // Possessive pronoun/determiner (my, your, his, her, its, our, their, whose)
    PRP_REF,   // Reflexive pronoun (myself, yourself, himself, herself, itself, ourselves, yourselves, themselves)
    WP,        // Wh-pronoun (who, whom, what, which)
    WRB,       // Wh-adverb (how, where, when, why)
    VB,        // Verb, base form (go, see, write, be, have, do)
    VBZ,       // Verb, 3rd person singular present (goes, sees, writes, is, has, does)
    VBP,       // Verb, non-3rd person singular present (go, see, write, am, are, have, do)
    VBD,       // Verb, past tense (went, saw, wrote, was, were, had, did)
    VBN,       // Verb, past participle (gone, seen, written, been, had, done)
    VBG,       // Verb, present participle/gerund (going, seeing, writing, being)
    MD,        // Modal (can, could, will, would, shall, should, may, might, must)
    JJ,        // Adjective (big, happy, clear)
    JJR,       // Adjective, comparative (bigger, happier, clearer)
    JJS,       // Adjective, superlative (biggest, happiest, clearest)
    RB,        // Adverb (quickly, clearly, very, not, never)
    RBR,       // Adverb, comparative (faster, harder)
    RBS,       // Adverb, superlative (fastest, hardest)
    DT,        // Determiner / Article (a, an, the, this, that, these, those, every, each, any, some, no)
    IN,        // Preposition or subordinating conjunction (in, on, at, by, with, although, because, since, if)
    CC,        // Coordinating conjunction (and, but, or, nor, so, yet, for)
    CD,        // Cardinal number (one, two, 100, 3.14)
    UH,        // Interjection (oh, wow, hello)
    POS,       // Possessive clitic ('s)
    PUNCT,     // Punctuation (. , ! ? ; : - " ' ( ) [ ])
    UNKNOWN    // Unclassified
}

data class Token(
    val text: String,
    val cleanText: String,
    val startOffset: Int,
    val endOffset: Int,
    val originalStartOffset: Int,
    val originalEndOffset: Int,
    val type: TokenType,
    val casing: WordCasing,
    val lemma: String? = null,
    val tag: POSTag = POSTag.UNKNOWN,
    val possibleTags: Set<POSTag> = emptySet()
) {
    val length: Int get() = endOffset - startOffset
    val isWord: Boolean get() = type == TokenType.WORD || type == TokenType.CONTRACTION
    val isPunctuation: Boolean get() = type == TokenType.PUNCTUATION
}

data class SentenceSpan(
    val start: Int,
    val end: Int,
    val text: String,
    val originalStart: Int,
    val originalEnd: Int
) {
    val length: Int get() = end - start
}

data class AnalyzedSentence(
    val span: SentenceSpan,
    val tokens: List<Token>,
    val fullText: String
) {
    val wordTokens: List<Token> by lazy { tokens.filter { it.isWord } }
}

data class GrammarError(
    val ruleId: String,
    val startOffset: Int,
    val endOffset: Int,
    val originalStartOffset: Int,
    val originalEndOffset: Int,
    val originalText: String,
    val suggestedReplacement: String?,
    val category: CorrectionCategory,
    val severity: CorrectionSeverity,
    val confidence: Float,
    val explanation: String,
    val isSafeToApply: Boolean = true
) {
    fun toCorrection(): Correction = Correction(
        id = UUID.randomUUID().toString(),
        startOffset = originalStartOffset,
        endOffset = originalEndOffset,
        originalText = originalText,
        suggestedReplacement = suggestedReplacement,
        category = category,
        explanation = explanation,
        severity = severity,
        confidence = confidence,
        ruleId = ruleId,
        isSafeToApply = isSafeToApply
    )
}

data class NormalizedResult(
    val normalizedText: String,
    val originalToNormalizedMap: IntArray,
    val normalizedToOriginalMap: IntArray
) {
    fun toOriginalOffset(normOffset: Int): Int {
        if (normOffset <= 0) return 0
        if (normOffset >= normalizedToOriginalMap.size) {
            return normalizedToOriginalMap.lastOrNull() ?: normOffset
        }
        return normalizedToOriginalMap[normOffset]
    }

    fun toNormalizedOffset(origOffset: Int): Int {
        if (origOffset <= 0) return 0
        if (origOffset >= originalToNormalizedMap.size) {
            return originalToNormalizedMap.lastOrNull() ?: origOffset
        }
        return originalToNormalizedMap[origOffset]
    }
}
