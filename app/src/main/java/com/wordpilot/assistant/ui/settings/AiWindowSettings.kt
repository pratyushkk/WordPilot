package com.wordpilot.assistant.ui.settings

import android.content.Context
import android.content.SharedPreferences

enum class WindowAnchorPosition(val displayName: String) {
    SNAP_TO_BUBBLE("Follow Floating Bubble"),
    ABOVE_KEYBOARD("Anchor Above Keyboard"),
    CENTER_DIALOG("Center Screen Modal"),
    BOTTOM_SHEET("Bottom Sheet Dock")
}

enum class WindowGlassPreset(val displayName: String, val opacity: Float, val blurDp: Float) {
    FROSTED_GLASS("Frosted Glass (Default)", 0.88f, 18f),
    DEEP_OBSIDIAN("Deep Obsidian", 0.96f, 24f),
    TRANSLUCENT("Subtle Minimal", 0.72f, 12f),
    PURE_OLED("Pitch Black OLED", 1.0f, 0f)
}

enum class StreamingSpeed(val displayName: String, val delayMs: Long) {
    INSTANT("Instant (No animation)", 0L),
    TYPEWRITER_FAST("Fast Typewriter (15ms)", 15L),
    TYPEWRITER_NATURAL("Natural Pace (30ms)", 30L)
}

data class AiWindowSettings(
    // Dimensions
    val windowWidthDp: Float = 340f,
    val windowHeightDp: Float = 460f,
    val cornerRadiusDp: Float = 26f,
    val borderWidthDp: Float = 1.2f,

    // Glass & Styling
    val glassPreset: WindowGlassPreset = WindowGlassPreset.FROSTED_GLASS,
    val backgroundTintArgb: Int = 0xFF12141A.toInt(),
    val accentBorderArgb: Int = 0xFF10B981.toInt(), // Emerald
    val borderGlowIntensity: Float = 0.45f,
    val backgroundOpacity: Float = 0.90f,

    // Position & Behavior
    val anchorPosition: WindowAnchorPosition = WindowAnchorPosition.SNAP_TO_BUBBLE,
    val closeOnOutsideTouch: Boolean = true,
    val autoClearChatOnClose: Boolean = false,
    val streamingSpeed: StreamingSpeed = StreamingSpeed.TYPEWRITER_FAST,

    // Enabled Features Matrix
    val showChatTab: Boolean = true,
    val showSummarizeTab: Boolean = true,
    val showSmartReplyTab: Boolean = true,
    val showTranslateTab: Boolean = true,
    val showExplainTab: Boolean = true,
    val enabledRewriteStyles: List<String> = listOf(
        "MAKE_PROFESSIONAL",
        "MAKE_CASUAL",
        "MAKE_CONCISE",
        "MAKE_ELABORATE",
        "FIX_GRAMMAR",
        "IMPROVE_CLARITY"
    ),

    // Quick Tone Chips
    val defaultToneChips: List<String> = listOf(
        "Professional", "Casual", "Assertive", "Friendly", "Urgent", "Persuasive"
    ),

    // Custom Quick Prompts
    val customPrompts: List<String> = listOf(
        "Turn into bullet points",
        "Make this sound confident",
        "Check for passive voice",
        "Reply politely saying no"
    )
) {
    companion object {
        const val PREFS_NAME = "wordpilot_ai_window_prefs"

        fun load(context: Context): AiWindowSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val anchorName = prefs.getString("ai_win_anchor", WindowAnchorPosition.SNAP_TO_BUBBLE.name)
                ?: WindowAnchorPosition.SNAP_TO_BUBBLE.name
            val presetName = prefs.getString("ai_win_preset", WindowGlassPreset.FROSTED_GLASS.name)
                ?: WindowGlassPreset.FROSTED_GLASS.name
            val speedName = prefs.getString("ai_win_speed", StreamingSpeed.TYPEWRITER_FAST.name)
                ?: StreamingSpeed.TYPEWRITER_FAST.name

            val anchor = try { WindowAnchorPosition.valueOf(anchorName) } catch (_: Exception) { WindowAnchorPosition.SNAP_TO_BUBBLE }
            val preset = try { WindowGlassPreset.valueOf(presetName) } catch (_: Exception) { WindowGlassPreset.FROSTED_GLASS }
            val speed = try { StreamingSpeed.valueOf(speedName) } catch (_: Exception) { StreamingSpeed.TYPEWRITER_FAST }

            return AiWindowSettings(
                windowWidthDp = prefs.getFloat("ai_win_width", 340f),
                windowHeightDp = prefs.getFloat("ai_win_height", 460f),
                cornerRadiusDp = prefs.getFloat("ai_win_radius", 26f),
                backgroundOpacity = prefs.getFloat("ai_win_opacity", 0.90f),
                anchorPosition = anchor,
                glassPreset = preset,
                streamingSpeed = speed,
                showChatTab = prefs.getBoolean("ai_win_show_chat", true),
                showSummarizeTab = prefs.getBoolean("ai_win_show_sum", true),
                showSmartReplyTab = prefs.getBoolean("ai_win_show_smart", true),
                showTranslateTab = prefs.getBoolean("ai_win_show_trans", true),
                showExplainTab = prefs.getBoolean("ai_win_show_exp", true)
            )
        }

        fun save(context: Context, settings: AiWindowSettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putFloat("ai_win_width", settings.windowWidthDp)
                putFloat("ai_win_height", settings.windowHeightDp)
                putFloat("ai_win_radius", settings.cornerRadiusDp)
                putFloat("ai_win_opacity", settings.backgroundOpacity)
                putString("ai_win_anchor", settings.anchorPosition.name)
                putString("ai_win_preset", settings.glassPreset.name)
                putString("ai_win_speed", settings.streamingSpeed.name)
                putBoolean("ai_win_show_chat", settings.showChatTab)
                putBoolean("ai_win_show_sum", settings.showSummarizeTab)
                putBoolean("ai_win_show_smart", settings.showSmartReplyTab)
                putBoolean("ai_win_show_trans", settings.showTranslateTab)
                putBoolean("ai_win_show_exp", settings.showExplainTab)
                apply()
            }
        }
    }
}
