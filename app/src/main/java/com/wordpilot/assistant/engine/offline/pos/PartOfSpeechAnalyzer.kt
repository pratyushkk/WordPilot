package com.wordpilot.assistant.engine.offline.pos

import com.wordpilot.assistant.engine.offline.dictionary.EnglishDictionary
import com.wordpilot.assistant.engine.offline.model.POSTag
import com.wordpilot.assistant.engine.offline.model.Token
import com.wordpilot.assistant.engine.offline.model.TokenType
import com.wordpilot.assistant.engine.offline.model.WordCasing

class PartOfSpeechAnalyzer(
    private val dictionary: EnglishDictionary
) {

    fun tagSentence(tokens: List<Token>): List<Token> {
        val wordTokens = tokens.filter { it.type != TokenType.WHITESPACE }
        val taggedTokens = ArrayList<Token>(tokens.size)

        // First pass: dictionary lookup
        val wordTagMap = mutableMapOf<Int, POSTag>()
        for (i in wordTokens.indices) {
            val t = wordTokens[i]
            val tag = when (t.type) {
                TokenType.PUNCTUATION -> POSTag.PUNCT
                TokenType.NUMBER -> POSTag.CD
                TokenType.URL, TokenType.EMAIL, TokenType.SYMBOL -> POSTag.UNKNOWN
                TokenType.WORD, TokenType.CONTRACTION -> {
                    val possible = dictionary.getTags(t.cleanText)
                    if (possible.size == 1) {
                        possible.first()
                    } else {
                        POSTag.UNKNOWN
                    }
                }
                else -> POSTag.UNKNOWN
            }
            wordTagMap[i] = tag
        }

        // Second pass: contextual disambiguation
        for (i in wordTokens.indices) {
            val t = wordTokens[i]
            if (t.type != TokenType.WORD && t.type != TokenType.CONTRACTION) continue

            val currentTag = wordTagMap[i] ?: POSTag.UNKNOWN
            if (currentTag != POSTag.UNKNOWN) continue

            val possible = dictionary.getTags(t.cleanText)
            val prev = if (i > 0) wordTokens[i - 1] else null
            val prevTag = if (i > 0) wordTagMap[i - 1] ?: POSTag.UNKNOWN else POSTag.UNKNOWN
            val next = if (i + 1 < wordTokens.size) wordTokens[i + 1] else null

            val resolvedTag = disambiguate(t, prev, prevTag, next, possible)
            wordTagMap[i] = resolvedTag
        }

        // Reconstruct full token stream including whitespace
        var wordIdx = 0
        for (t in tokens) {
            if (t.type == TokenType.WHITESPACE) {
                taggedTokens.add(t)
            } else {
                val tag = wordTagMap[wordIdx] ?: POSTag.UNKNOWN
                val possible = if (t.isWord) dictionary.getTags(t.cleanText) else emptySet()
                taggedTokens.add(
                    t.copy(
                        tag = tag,
                        possibleTags = possible,
                        lemma = dictionary.getEntry(t.cleanText)?.lemma ?: t.cleanText
                    )
                )
                wordIdx++
            }
        }

        return taggedTokens
    }

    private fun disambiguate(
        token: Token,
        prevToken: Token?,
        prevTag: POSTag,
        nextToken: Token?,
        possible: Set<POSTag>
    ): POSTag {
        val word = token.cleanText

        // 1. After modal verb (MD: can, could, will, would, should, must) -> base verb VB
        if (prevTag == POSTag.MD) {
            return POSTag.VB
        }

        // 2. After infinitive "to" -> base verb VB (if possible)
        if (prevToken != null && prevToken.cleanText == "to") {
            if (POSTag.VB in possible) return POSTag.VB
        }

        // 3. After have/has/had -> past participle VBN
        if (prevToken != null && prevToken.cleanText in setOf("have", "has", "had", "'ve", "'s", "'d")) {
            if (POSTag.VBN in possible) return POSTag.VBN
        }

        // 4. After determiner (DT: a, an, the, this, that, my, your, etc.)
        if (prevTag == POSTag.DT || prevTag == POSTag.PRP_POS) {
            // If next word is a noun, this word could be an adjective
            if (nextToken != null && (POSTag.NN in dictionary.getTags(nextToken.cleanText) || POSTag.NNS in dictionary.getTags(nextToken.cleanText))) {
                if (POSTag.JJ in possible) return POSTag.JJ
            }
            if (POSTag.NN in possible) return POSTag.NN
            if (POSTag.NNS in possible) return POSTag.NNS
        }

        // 5. After subject pronoun (he, she, it) -> 3rd person singular present VBZ or base verb VB (if erroneous)
        if (prevToken != null && prevToken.cleanText in setOf("he", "she", "it")) {
            if (POSTag.VBZ in possible) return POSTag.VBZ
            if (POSTag.VB in possible) return POSTag.VB
            if (POSTag.VBP in possible) return POSTag.VBP
        }

        // 6. After plural subject pronoun (they, we, you, I) -> non-3rd person present VBP or VBZ (if erroneous)
        if (prevToken != null && prevToken.cleanText in setOf("they", "we", "you", "i")) {
            if (POSTag.VBP in possible || POSTag.VB in possible) return POSTag.VBP
            if (POSTag.VBZ in possible) return POSTag.VBZ
        }

        // 7. After subject noun (NN, NNS, NNP) -> predicate verb
        if (prevTag in setOf(POSTag.NN, POSTag.NNS, POSTag.NNP)) {
            if (POSTag.VBZ in possible) return POSTag.VBZ
            if (POSTag.VBP in possible) return POSTag.VBP
            if (POSTag.VB in possible) return POSTag.VB
            if (POSTag.VBD in possible) return POSTag.VBD
        }

        // 8. Proper noun check: capitalized in the middle of a sentence
        if (prevToken != null && token.casing == WordCasing.TITLECASE && prevTag != POSTag.PUNCT) {
            return POSTag.NNP
        }

        // 8. Fallback heuristics based on common suffix
        return when {
            word.endsWith("ly") -> POSTag.RB
            word.endsWith("ing") -> if (POSTag.VBG in possible) POSTag.VBG else POSTag.NN
            word.endsWith("ed") -> if (POSTag.VBD in possible) POSTag.VBD else POSTag.JJ
            word.endsWith("s") && !word.endsWith("ss") -> if (POSTag.NNS in possible) POSTag.NNS else POSTag.VBZ
            POSTag.NN in possible -> POSTag.NN
            POSTag.VB in possible -> POSTag.VB
            POSTag.JJ in possible -> POSTag.JJ
            else -> possible.firstOrNull() ?: POSTag.NN
        }
    }
}
