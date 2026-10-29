package com.wordpilot.assistant.ui.suggestions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordpilot.assistant.domain.models.Correction

@Composable
fun SuggestionList(
    corrections: List<Correction>,
    onAccept: (Correction) -> Unit,
    onDismiss: (Correction) -> Unit,
    onAddToDictionary: ((Correction) -> Unit)? = null,
    onIgnoreRule: ((Correction) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (corrections.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "No issues found!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Your text looks great",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val errorCount = corrections.size
                Text(
                    "$errorCount issue${if (errorCount != 1) "s" else ""} found",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(corrections, key = { it.id }) { correction ->
            var expanded by remember { mutableStateOf(false) }
            SuggestionCard(
                correction = correction,
                onAccept = { onAccept(correction) },
                onDismiss = { onDismiss(correction) },
                onExpand = { expanded = true },
                onAddToDictionary = onAddToDictionary?.let { { it(correction) } },
                onIgnoreRule = onIgnoreRule?.let { { it(correction) } },
                expanded = expanded
            )
        }
    }
}
