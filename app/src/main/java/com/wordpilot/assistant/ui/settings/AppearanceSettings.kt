package com.wordpilot.assistant.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wordpilot.assistant.ui.theme.AssistantColors
import com.wordpilot.assistant.ui.theme.AssistantDimensions
import com.wordpilot.assistant.ui.theme.GradientConfig
import com.wordpilot.assistant.ui.theme.GradientPresets
import org.json.JSONArray

enum class AiLogoIcon(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    SPARKLE("sparkle", "Sparkle Star", "Classic AI constellation", Icons.Filled.AutoAwesome),
    WAND("wand", "Magic Wand", "Precise grammar enhancements", Icons.Filled.AutoFixHigh),
    BOLT("bolt", "Lightning Bolt", "High-speed instant corrections", Icons.Filled.Bolt),
    PEN("pen", "Writing Pen", "Editorial craftsmanship", Icons.Filled.Edit),
    BRAIN("brain", "AI Intelligence", "Neural context reasoning", Icons.Filled.Psychology),
    ROBOT("robot", "Smart Assistant", "Friendly AI companion", Icons.Filled.SmartToy),
    LIGHTBULB("lightbulb", "Idea & Insight", "Clear thinking and ideas", Icons.Filled.Lightbulb),
    ROCKET("rocket", "Productivity Rocket", "Fast typing and flow", Icons.Filled.RocketLaunch),
    DIAMOND("diamond", "Premium Diamond", "Polished and refined prose", Icons.Filled.Diamond),
    STAR_EMBLEM("star_emblem", "Star Emblem", "Recognized writing quality", Icons.Filled.Stars),
    QUILL("quill", "Editorial Quill", "Classic literature tone", Icons.Filled.HistoryEdu),
    CHECK("check", "Verified Check", "Error-free guarantee", Icons.Filled.CheckCircle);

    companion object {
        fun fromId(id: String): AiLogoIcon = values().find { it.id == id } ?: SPARKLE
    }
}

enum class BubbleShape(val displayName: String) {
    CIRCLE("Circle"),
    SQUIRCLE("Squircle"),
    ROUNDED_SQUARE("Rounded Square"),
    CUSTOM("Custom");

    fun toShape(sizeDp: Float, customRadiusDp: Float): Shape {
        return when (this) {
            CIRCLE -> CircleShape
            SQUIRCLE -> RoundedCornerShape((sizeDp * 0.38f).dp)
            ROUNDED_SQUARE -> RoundedCornerShape((sizeDp * 0.22f).dp)
            CUSTOM -> RoundedCornerShape(customRadiusDp.coerceIn(0f, sizeDp / 2).dp)
        }
    }
}

enum class BadgeLetterPosition(val displayName: String) {
    NONE("Number Only"),
    PREFIX("Prefix Letter (e.g. E2)"),
    SUFFIX("Suffix Letter (e.g. 2e)")
}

data class AppearanceSettings(
    // Bubble
    val bubbleShape: BubbleShape = BubbleShape.CIRCLE,
    val bubbleSizeDp: Float = 48f,
    val bubbleCornerRadiusDp: Float = 24f,
    val backgroundOpacity: Float = 1.0f,
    val iconOpacity: Float = 1.0f,
    val borderOpacity: Float = 0.35f,
    val borderWidthDp: Float = 1.0f,
    val shadowIntensity: Float = 0.5f,
    val iconSizeDp: Float = 24f,

    // Gradient
    val gradientId: String = GradientPresets.Default.id,
    val customGradientColors: List<Int> = emptyList(),
    val gradientAngle: Float = 45f,

    // Auto Dim
    val autoDimEnabled: Boolean = false,
    val autoDimDelaySeconds: Int = 5,
    val autoDimOpacity: Float = 0.35f,

    // Behavior
    val autoHideWhenKeyboardClosed: Boolean = true,

    // Glass Surface
    val glassOpacity: Float = AssistantColors.DefaultGlassOpacity,
    val glassTintArgb: Int = 0xFF17191F.toInt(),
    val glassCornerRadiusDp: Float = 28f,
    val glassBorderOpacity: Float = AssistantColors.DefaultBorderOpacity,
    val glassBorderWidthDp: Float = AssistantDimensions.DefaultBorderWidth.value,
    val glassHighlightOpacity: Float = AssistantColors.DefaultHighlightOpacity,
    val glassShadowElevationDp: Float = AssistantDimensions.DefaultElevation.value,

    // Toolbar Dimensions
    val toolbarHeightDp: Float = 50f,
    val toolbarMaxWidthDp: Float = 320f,
    val chipSpacingDp: Float = 6f,
    val chipIconSizeDp: Float = 16f,
    val chipLabelSizeSp: Float = 12f,

    // Quick Actions
    val activeQuickActions: List<String> = listOf("fix_grammar", "settings"),
    val moreActions: List<String> = emptyList(),

    // Animation & Timers
    val animationsEnabled: Boolean = true,
    val autoCollapseDelaySeconds: Int = 0,

    // Edge Docking & Tuck (30% hidden in edge when minimized)
    val edgeTuckEnabled: Boolean = true,
    val edgeTuckPercent: Float = 0.30f,

    // Error Badge & Letter Control
    val badgeLetterPosition: BadgeLetterPosition = BadgeLetterPosition.NONE,
    val badgeLetter: String = "E",
    val badgeTextSizeSp: Float = 18f,
    val badgeFontWeightBold: Boolean = true,

    // AI Logo in Editing Section
    val aiLogoIconId: String = "sparkle",

    // Expander Window Blur Design
    val expanderBlurEnabled: Boolean = true,
    val expanderBlurRadiusDp: Float = 20f
) {
    fun getAiLogo(): AiLogoIcon = AiLogoIcon.fromId(aiLogoIconId)

    fun getEffectiveGradient(): GradientConfig {
        if (customGradientColors.isNotEmpty() && gradientId == "custom") {
            return GradientConfig.fromColorIntList(
                id = "custom",
                name = "Custom Gradient",
                colorInts = customGradientColors,
                angle = gradientAngle
            )
        }
        return GradientPresets.findById(gradientId)
    }

    companion object {
        const val PREFS_NAME = "wordpilot_appearance_studio_prefs"

        private const val KEY_BUBBLE_SHAPE = "appr_bubble_shape"
        private const val KEY_BUBBLE_SIZE = "appr_bubble_size"
        private const val KEY_BUBBLE_CORNER_RADIUS = "appr_bubble_corner_radius"
        private const val KEY_BG_OPACITY = "appr_bg_opacity"
        private const val KEY_ICON_OPACITY = "appr_icon_opacity"
        private const val KEY_BORDER_OPACITY = "appr_border_opacity"
        private const val KEY_BORDER_WIDTH = "appr_border_width"
        private const val KEY_SHADOW_INTENSITY = "appr_shadow_intensity"
        private const val KEY_ICON_SIZE = "appr_icon_size"

        private const val KEY_GRADIENT_ID = "appr_gradient_id"
        private const val KEY_CUSTOM_GRADIENT_COLORS = "appr_custom_gradient_colors"
        private const val KEY_GRADIENT_ANGLE = "appr_gradient_angle"

        private const val KEY_AUTO_DIM_ENABLED = "appr_auto_dim_enabled"
        private const val KEY_AUTO_DIM_DELAY = "appr_auto_dim_delay"
        private const val KEY_AUTO_DIM_OPACITY = "appr_auto_dim_opacity"
        private const val KEY_AUTO_HIDE_KEYBOARD = "appr_auto_hide_keyboard"

        private const val KEY_GLASS_OPACITY = "appr_glass_opacity"
        private const val KEY_GLASS_TINT = "appr_glass_tint"
        private const val KEY_GLASS_CORNER_RADIUS = "appr_glass_corner_radius"
        private const val KEY_GLASS_BORDER_OPACITY = "appr_glass_border_opacity"
        private const val KEY_GLASS_BORDER_WIDTH = "appr_glass_border_width"
        private const val KEY_GLASS_HIGHLIGHT_OPACITY = "appr_glass_highlight_opacity"
        private const val KEY_GLASS_SHADOW_ELEVATION = "appr_glass_shadow_elevation"

        private const val KEY_TOOLBAR_HEIGHT = "appr_toolbar_height"
        private const val KEY_TOOLBAR_MAX_WIDTH = "appr_toolbar_max_width"
        private const val KEY_CHIP_SPACING = "appr_chip_spacing"
        private const val KEY_CHIP_ICON_SIZE = "appr_chip_icon_size"
        private const val KEY_CHIP_LABEL_SIZE = "appr_chip_label_size"

        private const val KEY_ACTIVE_QUICK_ACTIONS = "appr_active_quick_actions"
        private const val KEY_MORE_ACTIONS = "appr_more_actions"

        private const val KEY_ANIMATIONS_ENABLED = "appr_animations_enabled"
        private const val KEY_AUTO_COLLAPSE_DELAY = "appr_auto_collapse_delay"

        private const val KEY_EDGE_TUCK_ENABLED = "appr_edge_tuck_enabled"
        private const val KEY_EDGE_TUCK_PERCENT = "appr_edge_tuck_percent"
        private const val KEY_BADGE_LETTER_POSITION = "appr_badge_letter_position"
        private const val KEY_BADGE_LETTER = "appr_badge_letter"
        private const val KEY_BADGE_TEXT_SIZE_SP = "appr_badge_text_size_sp"
        private const val KEY_BADGE_FONT_WEIGHT_BOLD = "appr_badge_font_weight_bold"
        private const val KEY_AI_LOGO_ICON_ID = "appr_ai_logo_icon_id"

        fun load(context: Context): AppearanceSettings {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val shapeName = sp.getString(KEY_BUBBLE_SHAPE, BubbleShape.CIRCLE.name) ?: BubbleShape.CIRCLE.name
            val shape = try { BubbleShape.valueOf(shapeName) } catch (_: Exception) { BubbleShape.CIRCLE }

            val customColorsJson = sp.getString(KEY_CUSTOM_GRADIENT_COLORS, null)
            val customColors = mutableListOf<Int>()
            if (!customColorsJson.isNullOrEmpty()) {
                try {
                    val array = JSONArray(customColorsJson)
                    for (i in 0 until array.length()) {
                        customColors.add(array.getInt(i))
                    }
                } catch (_: Exception) {}
            }

            fun parseStringList(key: String, defaultList: List<String>): List<String> {
                val json = sp.getString(key, null) ?: return defaultList
                return try {
                    val arr = JSONArray(json)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getString(i))
                    }
                    if (list.isEmpty()) defaultList else list
                } catch (_: Exception) {
                    defaultList
                }
            }

            val defaultInst = AppearanceSettings()

            return AppearanceSettings(
                bubbleShape = shape,
                bubbleSizeDp = sp.getFloat(KEY_BUBBLE_SIZE, defaultInst.bubbleSizeDp),
                bubbleCornerRadiusDp = sp.getFloat(KEY_BUBBLE_CORNER_RADIUS, defaultInst.bubbleCornerRadiusDp),
                backgroundOpacity = sp.getFloat(KEY_BG_OPACITY, defaultInst.backgroundOpacity),
                iconOpacity = sp.getFloat(KEY_ICON_OPACITY, defaultInst.iconOpacity),
                borderOpacity = sp.getFloat(KEY_BORDER_OPACITY, defaultInst.borderOpacity),
                borderWidthDp = sp.getFloat(KEY_BORDER_WIDTH, defaultInst.borderWidthDp),
                shadowIntensity = sp.getFloat(KEY_SHADOW_INTENSITY, defaultInst.shadowIntensity),
                iconSizeDp = sp.getFloat(KEY_ICON_SIZE, defaultInst.iconSizeDp),

                gradientId = sp.getString(KEY_GRADIENT_ID, defaultInst.gradientId) ?: defaultInst.gradientId,
                customGradientColors = customColors,
                gradientAngle = sp.getFloat(KEY_GRADIENT_ANGLE, defaultInst.gradientAngle),

                autoDimEnabled = sp.getBoolean(KEY_AUTO_DIM_ENABLED, defaultInst.autoDimEnabled),
                autoDimDelaySeconds = sp.getInt(KEY_AUTO_DIM_DELAY, defaultInst.autoDimDelaySeconds),
                autoDimOpacity = sp.getFloat(KEY_AUTO_DIM_OPACITY, defaultInst.autoDimOpacity),
                autoHideWhenKeyboardClosed = sp.getBoolean(KEY_AUTO_HIDE_KEYBOARD, true),

                glassOpacity = sp.getFloat(KEY_GLASS_OPACITY, defaultInst.glassOpacity),
                glassTintArgb = sp.getInt(KEY_GLASS_TINT, defaultInst.glassTintArgb),
                glassCornerRadiusDp = sp.getFloat(KEY_GLASS_CORNER_RADIUS, defaultInst.glassCornerRadiusDp),
                glassBorderOpacity = sp.getFloat(KEY_GLASS_BORDER_OPACITY, defaultInst.glassBorderOpacity),
                glassBorderWidthDp = sp.getFloat(KEY_GLASS_BORDER_WIDTH, defaultInst.glassBorderWidthDp),
                glassHighlightOpacity = sp.getFloat(KEY_GLASS_HIGHLIGHT_OPACITY, defaultInst.glassHighlightOpacity),
                glassShadowElevationDp = sp.getFloat(KEY_GLASS_SHADOW_ELEVATION, defaultInst.glassShadowElevationDp),

                toolbarHeightDp = sp.getFloat(KEY_TOOLBAR_HEIGHT, defaultInst.toolbarHeightDp),
                toolbarMaxWidthDp = sp.getFloat(KEY_TOOLBAR_MAX_WIDTH, defaultInst.toolbarMaxWidthDp),
                chipSpacingDp = sp.getFloat(KEY_CHIP_SPACING, defaultInst.chipSpacingDp),
                chipIconSizeDp = sp.getFloat(KEY_CHIP_ICON_SIZE, defaultInst.chipIconSizeDp),
                chipLabelSizeSp = sp.getFloat(KEY_CHIP_LABEL_SIZE, defaultInst.chipLabelSizeSp),

                activeQuickActions = parseStringList(KEY_ACTIVE_QUICK_ACTIONS, defaultInst.activeQuickActions)
                    .filter { it == "fix_grammar" || it == "settings" }
                    .ifEmpty { listOf("fix_grammar", "settings") },
                moreActions = emptyList(),

                animationsEnabled = sp.getBoolean(KEY_ANIMATIONS_ENABLED, defaultInst.animationsEnabled),
                autoCollapseDelaySeconds = sp.getInt(KEY_AUTO_COLLAPSE_DELAY, defaultInst.autoCollapseDelaySeconds),

                edgeTuckEnabled = sp.getBoolean(KEY_EDGE_TUCK_ENABLED, defaultInst.edgeTuckEnabled),
                edgeTuckPercent = sp.getFloat(KEY_EDGE_TUCK_PERCENT, defaultInst.edgeTuckPercent),
                badgeLetterPosition = try {
                    BadgeLetterPosition.valueOf(sp.getString(KEY_BADGE_LETTER_POSITION, defaultInst.badgeLetterPosition.name) ?: defaultInst.badgeLetterPosition.name)
                } catch (_: Exception) {
                    BadgeLetterPosition.NONE
                },
                badgeLetter = sp.getString(KEY_BADGE_LETTER, defaultInst.badgeLetter) ?: defaultInst.badgeLetter,
                badgeTextSizeSp = sp.getFloat(KEY_BADGE_TEXT_SIZE_SP, defaultInst.badgeTextSizeSp),
                badgeFontWeightBold = sp.getBoolean(KEY_BADGE_FONT_WEIGHT_BOLD, defaultInst.badgeFontWeightBold),
                aiLogoIconId = sp.getString(KEY_AI_LOGO_ICON_ID, defaultInst.aiLogoIconId) ?: defaultInst.aiLogoIconId,
                expanderBlurEnabled = sp.getBoolean("appr_expander_blur_enabled", true),
                expanderBlurRadiusDp = sp.getFloat("appr_expander_blur_radius", 20f)
            )
        }

        fun save(context: Context, settings: AppearanceSettings) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = sp.edit()

            editor.putString(KEY_BUBBLE_SHAPE, settings.bubbleShape.name)
            editor.putFloat(KEY_BUBBLE_SIZE, settings.bubbleSizeDp)
            editor.putFloat(KEY_BUBBLE_CORNER_RADIUS, settings.bubbleCornerRadiusDp)
            editor.putFloat(KEY_BG_OPACITY, settings.backgroundOpacity)
            editor.putFloat(KEY_ICON_OPACITY, settings.iconOpacity)
            editor.putFloat(KEY_BORDER_OPACITY, settings.borderOpacity)
            editor.putFloat(KEY_BORDER_WIDTH, settings.borderWidthDp)
            editor.putFloat(KEY_SHADOW_INTENSITY, settings.shadowIntensity)
            editor.putFloat(KEY_ICON_SIZE, settings.iconSizeDp)

            editor.putString(KEY_GRADIENT_ID, settings.gradientId)
            val customColorsJson = JSONArray(settings.customGradientColors).toString()
            editor.putString(KEY_CUSTOM_GRADIENT_COLORS, customColorsJson)
            editor.putFloat(KEY_GRADIENT_ANGLE, settings.gradientAngle)

            editor.putBoolean(KEY_AUTO_DIM_ENABLED, settings.autoDimEnabled)
            editor.putInt(KEY_AUTO_DIM_DELAY, settings.autoDimDelaySeconds)
            editor.putFloat(KEY_AUTO_DIM_OPACITY, settings.autoDimOpacity)
            editor.putBoolean(KEY_AUTO_HIDE_KEYBOARD, settings.autoHideWhenKeyboardClosed)

            editor.putFloat(KEY_GLASS_OPACITY, settings.glassOpacity)
            editor.putInt(KEY_GLASS_TINT, settings.glassTintArgb)
            editor.putFloat(KEY_GLASS_CORNER_RADIUS, settings.glassCornerRadiusDp)
            editor.putFloat(KEY_GLASS_BORDER_OPACITY, settings.glassBorderOpacity)
            editor.putFloat(KEY_GLASS_BORDER_WIDTH, settings.glassBorderWidthDp)
            editor.putFloat(KEY_GLASS_HIGHLIGHT_OPACITY, settings.glassHighlightOpacity)
            editor.putFloat(KEY_GLASS_SHADOW_ELEVATION, settings.glassShadowElevationDp)

            editor.putFloat(KEY_TOOLBAR_HEIGHT, settings.toolbarHeightDp)
            editor.putFloat(KEY_TOOLBAR_MAX_WIDTH, settings.toolbarMaxWidthDp)
            editor.putFloat(KEY_CHIP_SPACING, settings.chipSpacingDp)
            editor.putFloat(KEY_CHIP_ICON_SIZE, settings.chipIconSizeDp)
            editor.putFloat(KEY_CHIP_LABEL_SIZE, settings.chipLabelSizeSp)

            editor.putString(KEY_ACTIVE_QUICK_ACTIONS, JSONArray(settings.activeQuickActions).toString())
            editor.putString(KEY_MORE_ACTIONS, JSONArray(settings.moreActions).toString())

            editor.putBoolean(KEY_ANIMATIONS_ENABLED, settings.animationsEnabled)
            editor.putInt(KEY_AUTO_COLLAPSE_DELAY, settings.autoCollapseDelaySeconds)

            editor.putBoolean(KEY_EDGE_TUCK_ENABLED, settings.edgeTuckEnabled)
            editor.putFloat(KEY_EDGE_TUCK_PERCENT, settings.edgeTuckPercent)
            editor.putString(KEY_BADGE_LETTER_POSITION, settings.badgeLetterPosition.name)
            editor.putString(KEY_BADGE_LETTER, settings.badgeLetter)
            editor.putFloat(KEY_BADGE_TEXT_SIZE_SP, settings.badgeTextSizeSp)
            editor.putBoolean(KEY_BADGE_FONT_WEIGHT_BOLD, settings.badgeFontWeightBold)
            editor.putString(KEY_AI_LOGO_ICON_ID, settings.aiLogoIconId)
            editor.putBoolean("appr_expander_blur_enabled", settings.expanderBlurEnabled)
            editor.putFloat("appr_expander_blur_radius", settings.expanderBlurRadiusDp)

            editor.apply()
        }
    }
}
