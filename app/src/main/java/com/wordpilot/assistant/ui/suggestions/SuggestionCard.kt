package com.wordpilot.assistant.ui.suggestions

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
import com.wordpilot.assistant.ui.theme.*

@Composable
fun SuggestionCard(
    correction: Correction,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onExpand: (() -> Unit)? = null,
    onAddToDictionary: (() -> Unit)? = null,
    onIgnoreRule: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    expanded: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Error count indicator
                CorrectionIndicator(
                    category = correction.category,
                    count = 1
                )

                // Label
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getCorrectionLabel(correction),
                        style = MaterialTheme.typography.labelMedium,
                        color = getCategoryColor(correction.category),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (correction.suggestedReplacement != null) {
                        Text(
                            text = correction.suggestedReplacement,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Explanation
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = correction.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "\"${correction.originalText}\" → \"${correction.suggestedReplacement ?: "..."}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (correction.suggestedReplacement != null && correction.isSafeToApply) {
                    Button(
                        onClick = onAccept,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Accept", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (onExpand != null && !expanded) {
                    OutlinedButton(
                        onClick = onExpand,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("More", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (expanded) {
                    if (onAddToDictionary != null && correction.category == CorrectionCategory.SPELLING) {
                        OutlinedButton(
                            onClick = onAddToDictionary,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Add to Dictionary", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (onIgnoreRule != null) {
                        TextButton(
                            onClick = onIgnoreRule,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Ignore", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CorrectionIndicator(
    category: CorrectionCategory,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(28.dp),
        shape = CircleShape,
        color = getCategoryColor(category)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (count > 9) "9+" else count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun getCategoryColor(category: CorrectionCategory): Color {
    return when (category) {
        CorrectionCategory.SPELLING -> SpellingErrorColor
        CorrectionCategory.GRAMMAR -> GrammarErrorColor
        CorrectionCategory.PUNCTUATION -> PunctuationWarningColor
        CorrectionCategory.STYLE -> StyleSuggestionColor
        CorrectionCategory.CAPITALIZATION -> CapitalizationColor
    }
}

private fun getCorrectionLabel(correction: Correction): String {
    return when (correction.category) {
        CorrectionCategory.SPELLING -> "Correct your spelling"
        CorrectionCategory.GRAMMAR -> "Grammar suggestion"
        CorrectionCategory.PUNCTUATION -> "Punctuation"
        CorrectionCategory.STYLE -> "Style suggestion"
        CorrectionCategory.CAPITALIZATION -> "Capitalization"
    }
}
