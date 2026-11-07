package com.wordpilot.assistant.ui.floating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.ui.settings.AiWindowSettings
import com.wordpilot.assistant.ui.settings.AppearanceSettings
import com.wordpilot.assistant.ui.theme.AssistantColors

data class ToneOption(
    val id: String,
    val style: RewriteStyle,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

/**
 * Universal catalog of all tone options available in WordPilot V2.
 * The top 4 (Fix & Polish, Short, Rewrite, Polite) are enabled by default on the floating panel.
 * Additional tones can be toggled by the user in Settings.
 */
val ALL_TONE_DEFINITIONS = listOf(
    ToneOption(
        id = "FIX_GRAMMAR",
        style = RewriteStyle.FIX_GRAMMAR,
        title = "Polish",
        subtitle = "Grammar & flow",
        icon = Icons.Filled.AutoAwesome,
        accentColor = Color(0xFF10B981)
    ),
    ToneOption(
        id = "MAKE_CONCISE",
        style = RewriteStyle.MAKE_CONCISE,
        title = "Short",
        subtitle = "Direct & concise",
        icon = Icons.Filled.Compress,
        accentColor = Color(0xFF34D399)
    ),
    ToneOption(
        id = "REWRITE",
        style = RewriteStyle.REWRITE,
        title = "Rewrite",
        subtitle = "Natural & expressive",
        icon = Icons.Filled.Refresh,
        accentColor = Color(0xFF60A5FA)
    ),
    ToneOption(
        id = "MAKE_POLITE",
        style = RewriteStyle.MAKE_POLITE,
        title = "Polite",
        subtitle = "Courteous & diplomatic",
        icon = Icons.Filled.FavoriteBorder,
        accentColor = Color(0xFFF472B6)
    ),
    ToneOption(
        id = "MAKE_PROFESSIONAL",
        style = RewriteStyle.MAKE_PROFESSIONAL,
        title = "Formal",
        subtitle = "Polished & formal",
        icon = Icons.Filled.WorkOutline,
        accentColor = Color(0xFF38BDF8)
    ),
    ToneOption(
        id = "MAKE_CASUAL",
        style = RewriteStyle.MAKE_CASUAL,
        title = "Casual",
        subtitle = "Warm & relaxed",
        icon = Icons.Filled.SentimentSatisfied,
        accentColor = Color(0xFFFBBF24)
    ),
    ToneOption(
        id = "IMPROVE_CLARITY",
        style = RewriteStyle.IMPROVE_CLARITY,
        title = "Clarity",
        subtitle = "Clear & easy to read",
        icon = Icons.Filled.Lightbulb,
        accentColor = Color(0xFFA78BFA)
    ),
    ToneOption(
        id = "IMPROVE_NATURAL",
        style = RewriteStyle.IMPROVE_NATURAL,
        title = "Natural",
        subtitle = "Native phrasing",
        icon = Icons.Filled.RecordVoiceOver,
        accentColor = Color(0xFFFB7185)
    )
)

@Composable
fun ToneSelectorPanel(
    settings: AppearanceSettings,
    selectedEngine: AIEngineType = AIEngineType.GEMINI_FLASH,
    onToneSelect: (RewriteStyle, String) -> Unit,
    onCustomPromptSubmit: ((String) -> Unit)? = null,
    onOpenGeminiSetup: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aiSettings = remember { AiWindowSettings.load(context) }
    val isGeminiConfigured = !WordPilotApp.instance.cachedGeminiApiKey.isNullOrBlank()

    // Filter tones based on user preferences in settings (default: strictly the 4 requested tones)
    val activeTones = remember(aiSettings.enabledToneStyles) {
        val enabledSet = aiSettings.enabledToneStyles.toSet()
        val filtered = ALL_TONE_DEFINITIONS.filter { it.id in enabledSet }
        if (filtered.isNotEmpty()) filtered else ALL_TONE_DEFINITIONS.take(4)
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
        shadowElevation = 18.dp,
        blurRadius = if (settings.expanderBlurEnabled) settings.expanderBlurRadiusDp.dp else 0.dp
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
                items(activeTones) { tone ->
                    Surface(
                        onClick = { onToneSelect(tone.style, tone.title) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E232E).copy(alpha = if (settings.expanderBlurEnabled) 0.50f else 0.70f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tone.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Direct Action to Customize / Select More Tones in Settings
            Surface(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E232E).copy(alpha = 0.55f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Customize Tones",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "More Tones & Options in Settings ➔",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
