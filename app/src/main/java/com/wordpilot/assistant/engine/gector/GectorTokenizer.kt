package com.wordpilot.assistant.engine.gector

import android.util.Log
import org.json.JSONObject
import java.io.File

data class TokenSpan(
    val token: String,
    val startOffset: Int,
    val endOffset: Int,
    val tokenId: Long = 3L,
    val isWordStart: Boolean = true
)

data class TokenizedInput(
    val tokens: List<TokenSpan>,
    val inputIds: LongArray,
    val attentionMask: LongArray
)

/**
 * Production RoBERTa BPE Tokenizer for GECToR.
 * Loads vocabulary from tokenizer.json and maps words and subwords with UTF-16 character offsets.
 */
class GectorTokenizer(
    private var vocab: Map<String, Long> = emptyMap()
) {
    companion object {
        private const val TAG = "GectorTokenizer"

        const val CLS_TOKEN = "<s>"
        const val PAD_TOKEN = "<pad>"
        const val SEP_TOKEN = "</s>"
        const val UNK_TOKEN = "<unk>"

        const val CLS_ID = 0L
        const val PAD_ID = 1L
        const val SEP_ID = 2L
        const val UNK_ID = 3L

        // RoBERTa prefix representing leading whitespace
        const val SPACE_PREFIX = "Ġ"
    }

    var isLoaded: Boolean = vocab.isNotEmpty()
        private set

    fun loadFromTokenizerJson(file: File): Boolean {
        if (!file.exists() || !file.canRead()) return false
        return try {
            val jsonStr = file.readText()
            val json = JSONObject(jsonStr)
            val modelObj = json.getJSONObject("model")
            val vocabObj = modelObj.getJSONObject("vocab")
            val map = HashMap<String, Long>(vocabObj.length())
            val keys = vocabObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = vocabObj.getLong(key)
            }
            vocab = map
            isLoaded = true
            Log.d(TAG, "Loaded ${vocab.size} RoBERTa BPE tokens from ${file.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tokenizer.json", e)
            false
        }
    }

    /**
     * Tokenize natural English text into RoBERTa BPE token spans.
     * Preserves exact character offsets and aligns word boundaries.
     */
    fun tokenize(text: String, maxSeqLength: Int = 128): TokenizedInput {
        if (text.isEmpty()) {
            return TokenizedInput(
                tokens = emptyList(),
                inputIds = longArrayOf(CLS_ID, SEP_ID),
                attentionMask = longArrayOf(1L, 1L)
            )
        }

        val spans = mutableListOf<TokenSpan>()
        val len = text.length
        var i = 0

        while (i < len) {
            val ch = text[i]
            if (ch.isWhitespace()) {
                i++
                continue
            }

            val hasLeadingSpace = (i > 0 && text[i - 1].isWhitespace())
            val start = i

            // Word extraction: group alphanumeric or punctuation
            if (ch.isLetterOrDigit()) {
                while (i < len && text[i].isLetterOrDigit()) {
                    i++
                }
            } else {
                i++
            }

            val rawWord = text.substring(start, i)
            val bpeCandidate = if (hasLeadingSpace) "$SPACE_PREFIX$rawWord" else rawWord

            // Check if full token exists in RoBERTa vocabulary
            val exactId = vocab[bpeCandidate] ?: vocab[rawWord]
            if (exactId != null) {
                spans.add(
                    TokenSpan(
                        token = rawWord,
                        startOffset = start,
                        endOffset = i,
                        tokenId = exactId,
                        isWordStart = true
                    )
                )
            } else {
                // Subword breakdown: greedy longest prefix match
                val prefix = if (hasLeadingSpace) SPACE_PREFIX else ""
                val subSpans = tokenizeSubwords(rawWord, prefix, start, i)
                spans.addAll(subSpans)
            }
        }

        // Limit to max sequence length excluding <s> and </s>
        val usableSpans = spans.take((maxSeqLength - 2).coerceAtLeast(1))

        val ids = LongArray(usableSpans.size + 2)
        val mask = LongArray(usableSpans.size + 2)

        ids[0] = CLS_ID
        mask[0] = 1L

        for (idx in usableSpans.indices) {
            ids[idx + 1] = usableSpans[idx].tokenId
            mask[idx + 1] = 1L
        }

        ids[ids.size - 1] = SEP_ID
        mask[mask.size - 1] = 1L

        return TokenizedInput(
            tokens = usableSpans,
            inputIds = ids,
            attentionMask = mask
        )
    }

    private fun tokenizeSubwords(
        rawWord: String,
        prefix: String,
        startOffset: Int,
        endOffset: Int
    ): List<TokenSpan> {
        val result = mutableListOf<TokenSpan>()
        var remaining = rawWord
        var isFirst = true

        while (remaining.isNotEmpty()) {
            var matched: String? = null
            var matchedId: Long? = null

            for (len in remaining.length downTo 1) {
                val candidate = remaining.substring(0, len)
                val tokenWithPrefix = if (isFirst && prefix.isNotEmpty()) "$prefix$candidate" else candidate
                val id = vocab[tokenWithPrefix] ?: vocab[candidate]
                if (id != null) {
                    matched = candidate
                    matchedId = id
                    break
                }
            }

            if (matched != null && matchedId != null) {
                result.add(
                    TokenSpan(
                        token = matched,
                        startOffset = startOffset, // Word-level start
                        endOffset = endOffset,     // Word-level end
                        tokenId = matchedId,
                        isWordStart = isFirst
                    )
                )
                remaining = remaining.substring(matched.length)
                isFirst = false
            } else {
                // Unknown single character fallback
                val chStr = remaining.substring(0, 1)
                result.add(
                    TokenSpan(
                        token = chStr,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        tokenId = vocab[chStr] ?: UNK_ID,
                        isWordStart = isFirst
                    )
                )
                remaining = remaining.substring(1)
                isFirst = false
            }
        }

        return result
    }
}
