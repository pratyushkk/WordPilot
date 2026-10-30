package com.wordpilot.assistant.ui.theme

import androidx.compose.ui.graphics.Color

object AssistantColors {
    // Premium dark smoked glass palette
    val SmokedGlassBase = Color(0xFF17191F)
    const val DefaultGlassOpacity = 0.82f
    const val DefaultBlurRadiusDp = 24f
    
    // Glass borders and highlights
    val GlassBorder = Color(0xFFFFFFFF)
    const val DefaultBorderOpacity = 0.15f
    const val DefaultBorderWidthDp = 1.0f
    
    val ReflectiveHighlight = Color(0xFFFFFFFF)
    const val DefaultHighlightOpacity = 0.22f

    // Action chips (neutral translucent)
    val ChipBackground = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val ChipBackgroundPressed = Color(0xFFFFFFFF).copy(alpha = 0.16f)
    val ChipBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f)
    val ChipText = Color(0xFFF3F4F6)

    // Primary, secondary, and accent texts
    val TextPrimary = Color(0xFFF9FAFB)
    val TextSecondary = Color(0xFF9CA3AF)
    val TextMuted = Color(0xFF6B7280)
    val AccentCyan = Color(0xFF00D2FF)
    val AccentBlue = Color(0xFF38BDF8)
    val AccentGreen = Color(0xFF10B981)

    // Category badge indicators
    val BadgeSpelling = Color(0xFFEF4444)
    val BadgeGrammar = Color(0xFF3B82F6)
    val BadgePunctuation = Color(0xFF8B5CF6)
    val BadgeStyle = Color(0xFF10B981)
    val BadgeCapitalization = Color(0xFFF59E0B)

    // Preview diff colors
    val DiffOriginalBackground = Color(0xFFEF4444).copy(alpha = 0.15f)
    val DiffOriginalText = Color(0xFFFCA5A5)
    val DiffImprovedBackground = Color(0xFF10B981).copy(alpha = 0.15f)
    val DiffImprovedText = Color(0xFF6EE7B7)
}
