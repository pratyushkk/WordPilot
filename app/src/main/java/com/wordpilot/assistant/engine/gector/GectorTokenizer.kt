package com.wordpilot.assistant.engine.gector

data class TokenizedInput(
    val tokens: List<TokenSpan>,
    val inputIds: LongArray,
    val attentionMask: LongArray
)

class GectorTokenizer(
    private val vocab: Map<String, Long> = emptyMap()
) {
    companion object {
        const val CLS_TOKEN = "[CLS]"
        const val SEP_TOKEN = "[SEP]"
        const val UNK_TOKEN = "[UNK]"
        const val PAD_TOKEN = "[PAD]"

        const val CLS_ID = 101L
        const val SEP_ID = 102L
        const val UNK_ID = 100L
        const val PAD_ID = 0L
    }

    fun tokenize(text: String, maxSeqLength: Int = 128): TokenizedInput {
        val spans = mutableListOf<TokenSpan>()
        val len = text.length
        var i = 0

        while (i < len) {
            val ch = text[i]
            if (ch.isWhitespace()) {
                i++
                continue
            }

            val start = i
            if (ch.isLetterOrDigit()) {
                while (i < len && text[i].isLetterOrDigit()) {
                    i++
                }
            } else {
                i++
            }
            val tokenStr = text.substring(start, i)
            spans.add(TokenSpan(token = tokenStr, startOffset = start, endOffset = i))
        }

        // Truncate to fit within [CLS] ... [SEP]
        val usableSpans = spans.take(maxSeqLength - 2)

        val ids = LongArray(usableSpans.size + 2)
        val mask = LongArray(usableSpans.size + 2)

        ids[0] = CLS_ID
        mask[0] = 1L

        for (idx in usableSpans.indices) {
            val token = usableSpans[idx].token.lowercase()
            val id = vocab[token] ?: UNK_ID
            ids[idx + 1] = id
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
}
