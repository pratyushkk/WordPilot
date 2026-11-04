package com.wordpilot.assistant.ui.floating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.ui.settings.AppearanceSettings
import com.wordpilot.assistant.ui.theme.AssistantColors

data class MoreOptionItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val isPrimary: Boolean = false
)

@Composable
fun MoreOptionsPanel(
    settings: AppearanceSettings,
    onActionClick: (String) -> Unit,
    onCustomPromptSubmit: (String) -> Unit = {},
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    selectedEngine: AIEngineType = AIEngineType.GEMINI_FLASH,
    onSelectEngine: (AIEngineType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var customPromptText by remember { mutableStateOf("") }
    val isGeminiConfigured = !WordPilotApp.instance.cachedGeminiApiKey.isNullOrBlank()

    val aiTools = listOf(
        MoreOptionItem("ai_chat", "AI Chat", Icons.Filled.ChatBubbleOutline, isPrimary = true),
        MoreOptionItem("summarize", "Summarize", Icons.Filled.Summarize),
        MoreOptionItem("smart_reply", "Smart Reply", Icons.Filled.Quickreply),
        MoreOptionItem("translate", "Translate", Icons.Filled.Translate),
        MoreOptionItem("explain", "Explain", Icons.Filled.Info)
    )

    val rewriteStyles = listOf(
        MoreOptionItem("rewrite_professional", "Professional", Icons.Filled.WorkOutline),
        MoreOptionItem("rewrite_casual", "Casual", Icons.Filled.SentimentSatisfied),
        MoreOptionItem("rewrite_concise", "Concise", Icons.Filled.Compress),
        MoreOptionItem("rewrite_clarity", "Clarity", Icons.Filled.Lightbulb),
        MoreOptionItem("rewrite_friendly", "Friendly", Icons.Filled.FavoriteBorder),
        MoreOptionItem("fix_grammar", "Fix Grammar", Icons.Filled.Spellcheck)
    )

    GlassBackground(
        modifier = modifier
            .widthIn(max = 340.dp)
            .wrapContentHeight(),
        opacity = settings.glassOpacity,
        baseColor = Color(settings.glassTintArgb),
        cornerRadius = settings.glassCornerRadiusDp.dp,
        borderWidth = settings.glassBorderWidthDp.dp,
        borderOpacity = settings.glassBorderOpacity,
        highlightOpacity = settings.glassHighlightOpacity,
        shadowElevation = settings.glassShadowElevationDp.dp
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
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "WordPilot AI",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WordPilot AI Studio",
                        color = AssistantColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Settings",
                            tint = AssistantColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = AssistantColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Missing API Key Banner
            if (!isGeminiConfigured) {
                Surface(
                    onClick = { onActionClick("gemini_setup") },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini API Key Required",
                                color = Color(0xFFFDE68A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap here to paste or enter your key",
                                color = AssistantColors.TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Section 1: AI Tools Bar
            Text(
                text = "AI TOOLS",
                color = Color(0xFF10B981),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                aiTools.forEach { tool ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (tool.isPrimary) Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))
                                else Brush.linearGradient(listOf(Color(0xFF1E232E), Color(0xFF181C24))),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                0.8.dp,
                                if (tool.isPrimary) Color(0xFF34D399) else Color(0xFF374151),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (!isGeminiConfigured) {
                                    onActionClick("gemini_setup")
                                } else {
                                    onActionClick(tool.id)
                                }
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = if (tool.isPrimary) Color.Black else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tool.title,
                                color = if (tool.isPrimary) Color.Black else AssistantColors.TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = if (tool.isPrimary) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Rewrite Tone Styles
            Text(
                text = "REWRITE STYLE",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rewriteStyles.chunked(2).forEach { rowStyles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowStyles.forEach { style ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF191D26), RoundedCornerShape(8.dp))
                                    .border(0.8.dp, Color(0xFF2E3440), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!isGeminiConfigured) {
                                            onActionClick("gemini_setup")
                                        } else {
                                            onActionClick(style.id)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = style.icon,
                                        contentDescription = style.title,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = style.title,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (rowStyles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Custom Instruction Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF11141A), RoundedCornerShape(12.dp))
                    .border(0.8.dp, Color(0xFF2E3440), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = customPromptText,
                    onValueChange = { customPromptText = it },
                    placeholder = {
                        Text(
                            text = "Custom instruction (e.g. make poetic)…",
                            color = AssistantColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF10B981),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (customPromptText.isNotBlank()) {
                            if (!isGeminiConfigured) {
                                onActionClick("gemini_setup")
                            } else {
                                val prompt = customPromptText.trim()
                                customPromptText = ""
                                onCustomPromptSubmit(prompt)
                            }
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (customPromptText.isNotBlank()) {
                            if (!isGeminiConfigured) {
                                onActionClick("gemini_setup")
                            } else {
                                val prompt = customPromptText.trim()
                                customPromptText = ""
                                onCustomPromptSubmit(prompt)
                            }
                        }
                    },
                    modifier = Modifier.size(28.dp),
                    enabled = customPromptText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Apply Custom",
                        tint = if (customPromptText.isNotBlank()) Color(0xFF10B981) else AssistantColors.TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
