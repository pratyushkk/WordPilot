package com.wordpilot.assistant.ui.floating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.ui.settings.AppearanceSettings
import com.wordpilot.assistant.ui.theme.AssistantColors

data class ToneOption(
    val style: RewriteStyle,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun ToneSelectorPanel(
    settings: AppearanceSettings,
    selectedEngine: AIEngineType = AIEngineType.GEMINI_FLASH,
    onToneSelect: (RewriteStyle, String) -> Unit,
    onCustomPromptSubmit: (String) -> Unit,
    onOpenGeminiSetup: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customPromptText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val isGeminiConfigured = !WordPilotApp.instance.cachedGeminiApiKey.isNullOrBlank()

    val tones = remember {
        listOf(
            ToneOption(
                style = RewriteStyle.MAKE_PROFESSIONAL,
                title = "Professional",
                subtitle = "Polished, formal & business-ready",
                icon = Icons.Filled.WorkOutline,
                accentColor = Color(0xFF60A5FA)
            ),
            ToneOption(
                style = RewriteStyle.MAKE_CASUAL,
                title = "Casual & Friendly",
                subtitle = "Warm, relaxed & conversational",
                icon = Icons.Filled.SentimentSatisfied,
                accentColor = Color(0xFFFBBF24)
            ),
            ToneOption(
                style = RewriteStyle.MAKE_CONCISE,
                title = "Concise",
                subtitle = "Direct, short & cuts filler",
                icon = Icons.Filled.Compress,
                accentColor = Color(0xFF34D399)
            ),
            ToneOption(
                style = RewriteStyle.IMPROVE_CLARITY,
                title = "Clarity",
                subtitle = "Clear, crisp & easy to read",
                icon = Icons.Filled.Lightbulb,
                accentColor = Color(0xFFA78BFA)
            ),
            ToneOption(
                style = RewriteStyle.MAKE_POLITE,
                title = "Make Polite",
                subtitle = "Courteous & diplomatic",
                icon = Icons.Filled.FavoriteBorder,
                accentColor = Color(0xFFF472B6)
            ),
            ToneOption(
                style = RewriteStyle.FIX_GRAMMAR,
                title = "Fix & Polish",
                subtitle = "Refine grammar, spelling & flow",
                icon = Icons.Filled.AutoAwesome,
                accentColor = Color(0xFF10B981)
            )
        )
    }

    GlassBackground(
        modifier = modifier
            .widthIn(max = 340.dp)
            .wrapContentHeight(),
        opacity = settings.glassOpacity.coerceAtLeast(0.85f),
        baseColor = Color(settings.glassTintArgb),
        cornerRadius = settings.glassCornerRadiusDp.dp,
        borderWidth = settings.glassBorderWidthDp.dp,
        borderOpacity = settings.glassBorderOpacity.coerceAtLeast(0.35f),
        highlightOpacity = settings.glassHighlightOpacity,
        shadowElevation = 18.dp
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Rewrite & Tone",
                            color = AssistantColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isGeminiConfigured) "Gemini Model Active" else "Offline Rules Active",
                            color = if (isGeminiConfigured) Color(0xFF34D399) else AssistantColors.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isGeminiConfigured) {
                        Surface(
                            onClick = onOpenGeminiSetup,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Setup Key",
                                color = Color(0xFFA5B4FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = AssistantColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tone Options Grid (2 columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                items(tones) { tone ->
                    Surface(
                        onClick = { onToneSelect(tone.style, tone.title) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E232E).copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(tone.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tone.icon,
                                    contentDescription = null,
                                    tint = tone.accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tone.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = tone.subtitle,
                                    color = AssistantColors.TextMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Instruction / Prompt Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = customPromptText,
                    onValueChange = { customPromptText = it },
                    placeholder = {
                        Text(
                            "Custom tone: e.g. persuasive...",
                            color = AssistantColors.TextMuted,
                            fontSize = 11.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (customPromptText.isNotBlank()) {
                                focusManager.clearFocus()
                                onCustomPromptSubmit(customPromptText.trim())
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AssistantColors.AccentCyan,
                        unfocusedBorderColor = AssistantColors.Divider,
                        focusedContainerColor = Color(0xFF1E232E).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(0xFF1E232E).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (customPromptText.isNotBlank()) {
                            focusManager.clearFocus()
                            onCustomPromptSubmit(customPromptText.trim())
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (customPromptText.isNotBlank())
                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)))
                            else
                                Brush.linearGradient(listOf(Color(0xFF374151), Color(0xFF1F2937)))
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (customPromptText.isNotBlank()) Color.White else AssistantColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
