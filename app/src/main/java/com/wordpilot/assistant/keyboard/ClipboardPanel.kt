package com.wordpilot.assistant.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClipboardPanel(
    clips: List<String>,
    onPasteClip: (String) -> Unit,
    onClearClips: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val itemBg = if (isDark) Color(0xFF282C34) else Color(0xFFFFFFFF)
    val itemBorder = if (isDark) Color(0xFF3E4451) else Color(0xFFE2E4E8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(265.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Clipboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (clips.isNotEmpty()) {
                IconButton(onClick = onClearClips, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Copied text will appear here.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(clips) { clipText ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = itemBg,
                        border = BorderStroke(1.dp, itemBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPasteClip(clipText) }
                    ) {
                        Text(
                            text = clipText,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
