package com.wordpilot.assistant.keyboard.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SuggestionDiffView(
    originalText: String,
    rewrittenText: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val annotatedDiff = remember(originalText, rewrittenText, isDark) {
        buildAnnotatedDiff(originalText, rewrittenText, isDark)
    }

    BasicText(
        text = annotatedDiff,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDark) Color(0xFF1E2024) else Color(0xFFF1F3F5)
            )
            .padding(12.dp)
    )
}

private fun buildAnnotatedDiff(
    original: String,
    rewritten: String,
    isDark: Boolean
) = buildAnnotatedString {
    val origWords = original.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val newWords = rewritten.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    val addedColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val removedColor = if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
    val normalColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF212121)

    // Compute simple LCS-based diff for words
    val lcs = computeLcs(origWords, newWords)
    var i = 0
    var j = 0
    var lcsIdx = 0

    while (i < origWords.size || j < newWords.size) {
        if (lcsIdx < lcs.size && i < origWords.size && j < newWords.size &&
            origWords[i] == lcs[lcsIdx] && newWords[j] == lcs[lcsIdx]
        ) {
            // Unchanged word
            withStyle(
                SpanStyle(
                    color = normalColor,
                    fontSize = 14.5.sp
                )
            ) {
                append("${newWords[j]} ")
            }
            i++
            j++
            lcsIdx++
        } else {
            // Removed words from original
            while (i < origWords.size && (lcsIdx >= lcs.size || origWords[i] != lcs[lcsIdx])) {
                withStyle(
                    SpanStyle(
                        color = removedColor,
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = 13.5.sp
                    )
                ) {
                    append("${origWords[i]} ")
                }
                i++
            }
            // Added words in rewritten
            while (j < newWords.size && (lcsIdx >= lcs.size || newWords[j] != lcs[lcsIdx])) {
                withStyle(
                    SpanStyle(
                        color = addedColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    )
                ) {
                    append("${newWords[j]} ")
                }
                j++
            }
        }
    }
}

private fun computeLcs(a: List<String>, b: List<String>): List<String> {
    val m = a.size
    val n = b.size
    val dp = Array(m + 1) { IntArray(n + 1) }

    for (i in 0 until m) {
        for (j in 0 until n) {
            if (a[i].equals(b[j], ignoreCase = false)) {
                dp[i + 1][j + 1] = dp[i][j] + 1
            } else {
                dp[i + 1][j + 1] = maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
    }

    val result = mutableListOf<String>()
    var i = m
    var j = n
    while (i > 0 && j > 0) {
        if (a[i - 1].equals(b[j - 1], ignoreCase = false)) {
            result.add(a[i - 1])
            i--
            j--
        } else if (dp[i - 1][j] > dp[i][j - 1]) {
            i--
        } else {
            j--
        }
    }
    return result.reversed()
}
