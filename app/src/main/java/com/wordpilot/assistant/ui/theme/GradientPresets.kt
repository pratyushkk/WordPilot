package com.wordpilot.assistant.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class GradientConfig(
    val id: String,
    val name: String,
    val colors: List<Color>,
    val angleDegrees: Float = 45f,
    val isCustom: Boolean = false
) {
    /**
     * Creates a full-bleed diagonal gradient that adapts dynamically
     * to the exact width and height of any drawing container (from 20dp swatches
     * to 56dp floating bubbles and larger preview surfaces).
     */
    fun createBrush(): Brush {
        return Brush.linearGradient(
            colors = colors,
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    /**
     * Horizontal gradient across component width, ideal for swatch preview pills.
     */
    fun createHorizontalBrush(): Brush {
        return Brush.horizontalGradient(
            colors = colors
        )
    }

    fun toColorIntList(): List<Int> = colors.map { it.toArgbCompat() }

    companion object {
        private fun Color.toArgbCompat(): Int {
            return android.graphics.Color.argb(
                (alpha * 255).toInt(),
                (red * 255).toInt(),
                (green * 255).toInt(),
                (blue * 255).toInt()
            )
        }

        fun fromColorIntList(id: String, name: String, colorInts: List<Int>, angle: Float = 45f): GradientConfig {
            return GradientConfig(
                id = id,
                name = name,
                colors = colorInts.map { Color(it) },
                angleDegrees = angle,
                isCustom = true
            )
        }
    }
}

object GradientPresets {
    // Kept Preset 1: Aurora (Neon Mint -> Electric Cyan -> Radiant Azure Blue)
    val Aurora = GradientConfig(
        id = "aurora",
        name = "Aurora",
        colors = listOf(
            Color(0xFF00F5A0),
            Color(0xFF00D9F5),
            Color(0xFF0072FF)
        ),
        angleDegrees = 45f
    )

    // Kept Preset 2: Ocean (Aqua Cyan -> Ocean Blue -> Electric Indigo)
    val Ocean = GradientConfig(
        id = "ocean",
        name = "Ocean",
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF2979FF),
            Color(0xFF651FFF)
        ),
        angleDegrees = 45f
    )

    // Vibrant Preset 3: Sunset Blaze (Solar Gold -> Fiery Orange -> Neon Magenta)
    val Sunset = GradientConfig(
        id = "sunset",
        name = "Sunset Blaze",
        colors = listOf(
            Color(0xFFFFD200),
            Color(0xFFFF5E36),
            Color(0xFFE02476)
        ),
        angleDegrees = 45f
    )

    // Vibrant Preset 4: Cosmic Violet (Hot Cyber Pink -> Electric Purple -> Azure Blue)
    val Cosmic = GradientConfig(
        id = "cosmic",
        name = "Cosmic Violet",
        colors = listOf(
            Color(0xFFFF007F),
            Color(0xFF8B5CF6),
            Color(0xFF3B82F6)
        ),
        angleDegrees = 45f
    )

    // Vibrant Preset 5: Cyberpunk (Electric Cyan -> Laser Magenta -> Electric Purple)
    val Cyberpunk = GradientConfig(
        id = "cyberpunk",
        name = "Cyberpunk",
        colors = listOf(
            Color(0xFF00F0FF),
            Color(0xFFFF006E),
            Color(0xFF8338EC)
        ),
        angleDegrees = 45f
    )

    // Vibrant Preset 6: Solar Flare (Citrus Gold -> Vivid Tangerine -> Crimson Ruby)
    val Solar = GradientConfig(
        id = "solar",
        name = "Solar Flare",
        colors = listOf(
            Color(0xFFFFB703),
            Color(0xFFFB8500),
            Color(0xFFD00000)
        ),
        angleDegrees = 45f
    )

    // Vibrant Preset 7: Emerald Glow (Lime Mint -> Vibrant Emerald -> Deep Teal)
    val Emerald = GradientConfig(
        id = "emerald",
        name = "Emerald Glow",
        colors = listOf(
            Color(0xFF34D399),
            Color(0xFF059669),
            Color(0xFF0F766E)
        ),
        angleDegrees = 45f
    )

    // Vibrant Preset 8: Prism (Cyan -> Indigo -> Magenta -> Warm Amber)
    val Prism = GradientConfig(
        id = "prism",
        name = "Prism",
        colors = listOf(
            Color(0xFF06B6D4),
            Color(0xFF6366F1),
            Color(0xFFEC4899),
            Color(0xFFF59E0B)
        ),
        angleDegrees = 45f
    )

    val Default = Aurora

    val AllPresets: List<GradientConfig> = listOf(
        Aurora,
        Ocean,
        Sunset,
        Cosmic,
        Cyberpunk,
        Solar,
        Emerald,
        Prism
    )

    fun findById(id: String): GradientConfig {
        return AllPresets.find { it.id.equals(id, ignoreCase = true) }
            ?: when (id.lowercase()) {
                "default", "default_signature" -> Aurora
                "neon", "fire" -> Sunset
                "midnight", "rose" -> Cosmic
                else -> Aurora
            }
    }
}
