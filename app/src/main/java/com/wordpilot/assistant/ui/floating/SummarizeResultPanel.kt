package com.wordpilot.assistant.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.ui.settings.AiWindowSettings
import com.wordpilot.assistant.ui.theme.AssistantColors

@Composable
fun SummarizeResultPanel(
    summaryText: String,
    isLoading: Boolean,
    errorMessage: String?,
    onReplace: (String) -> Unit,
    onInsert: (String) -> Unit,
    onCopy: (String) -> Unit,
    onRegenerate: (String) -> Unit,
    onClose: () -> Unit,
    aiWindowSettings: AiWindowSettings,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf("BULLETS") }
    val modes = listOf(
        "BULLETS" to "Bullets",
        "KEY_TAKEAWAYS" to "Takeaways",
        "ONE_SENTENCE" to "1-Sentence"
    )

    GlassBackground(
        modifier = modifier
            .width(aiWindowSettings.windowWidthDp.dp)
            .wrapContentHeight(),
        opacity = aiWindowSettings.backgroundOpacity,
        baseColor = Color(aiWindowSettings.backgroundTintArgb),
        cornerRadius = aiWindowSettings.cornerRadiusDp.dp,
        borderWidth = aiWindowSettings.borderWidthDp.dp,
        borderOpacity = 0.40f,
        highlightOpacity = 0.20f,
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Summarize,
                        contentDescription = "Summarize",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Summary",
                        color = AssistantColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = AssistantColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                modes.forEach { (modeKey, modeLabel) ->
                    val isSelected = selectedMode == modeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E232E),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 0.8.dp,
                                color = if (isSelected) Color(0xFF34D399) else Color(0xFF374151),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedMode = modeKey
                                onRegenerate(modeKey)
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modeLabel,
                            color = if (isSelected) Color.Black else AssistantColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 220.dp)
                    .background(Color(0xFF13171F), RoundedCornerShape(14.dp))
                    .border(0.8.dp, Color(0xFF262C38), RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generating summary…",
                            color = AssistantColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                } else if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = AssistantColors.TextError,
                        fontSize = 11.sp
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = summaryText,
                            color = Color(0xFFF3F4F6),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Replace / Insert / Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onReplace(summaryText) },
                    enabled = !isLoading && summaryText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Replace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onInsert(summaryText) },
                    enabled = !isLoading && summaryText.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF374151))
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Insert", fontSize = 11.sp)
                }

                IconButton(
                    onClick = { onCopy(summaryText) },
                    enabled = !isLoading && summaryText.isNotBlank(),
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1F2430), RoundedCornerShape(12.dp))
                        .border(0.8.dp, Color(0xFF374151), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        tint = AssistantColors.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
