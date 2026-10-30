package com.wordpilot.assistant.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GboardToolbar(
    suggestions: List<String>,
    onSuggestionClicked: (String) -> Unit,
    onAssistantClicked: () -> Unit,
    onResizeClicked: () -> Unit,
    onTextEditingClicked: () -> Unit,
    onClipboardClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left Gboard Tools Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // WordPilot Sparkle AI Assistant
                ToolbarIcon(
                    icon = Icons.Default.AutoAwesome,
                    contentDescription = "WordPilot AI",
                    tint = Color(0xFF4285F4),
                    onClick = onAssistantClicked
                )

                // Resize Keyboard (Gboard Resizer)
                ToolbarIcon(
                    icon = Icons.Default.AspectRatio,
                    contentDescription = "Resize Keyboard",
                    onClick = onResizeClicked
                )

                // Text Editing Mode ("Edit keyboard cursor controller")
                ToolbarIcon(
                    icon = Icons.Outlined.EditNote,
                    contentDescription = "Edit Text",
                    onClick = onTextEditingClicked
                )

                // Clipboard
                ToolbarIcon(
                    icon = Icons.Outlined.ContentPaste,
                    contentDescription = "Clipboard",
                    onClick = onClipboardClicked
                )

                // Edit Keyboard / Settings
                ToolbarIcon(
                    icon = Icons.Outlined.Tune,
                    contentDescription = "Edit Keyboard Settings",
                    onClick = onSettingsClicked
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 2. Center: Word Suggestions or Spacer
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (suggestions.isNotEmpty()) {
                    for (word in suggestions.take(3)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .clickable { onSuggestionClicked(word) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = word,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = "WordPilot Keyboard",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // 3. Right: Hide Keyboard Icon
            ToolbarIcon(
                icon = Icons.Default.KeyboardHide,
                contentDescription = "Hide Keyboard",
                onClick = onHideKeyboard
            )
        }
    }
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}
