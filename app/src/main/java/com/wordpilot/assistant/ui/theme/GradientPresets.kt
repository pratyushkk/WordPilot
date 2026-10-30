package com.wordpilot.assistant.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

data class GradientConfig(
    val id: String,
    val name: String,
    val colors: List<Color>,
    val angleDegrees: Float = 45f,
    val isCustom: Boolean = false
) {
    fun createBrush(): Brush {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val x = cos(rad).toFloat()
        val y = sin(rad).toFloat()
        
        return Brush.linearGradient(
            colors = colors,
            start = Offset(0f, 0f),
            end = Offset(x * 1000f, y * 1000f)
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
    // Default signature 4-color diagonal gradient: Cyan -> Blue -> Violet -> Pink
    val Default = GradientConfig(
        id = "default_signature",
        name = "Signature Sparkle",
        colors = listOf(
            Color(0xFF00D2FF), // Electric Cyan
            Color(0xFF3478F6), // Vibrant Blue
            Color(0xFFA855F7), // Violet
            Color(0xFFFF6B9D)  // Bright Pink
        ),
        angleDegrees = 45f
    )

    // Preset 1: Ocean (Cyan -> Blue -> Indigo)
    val Ocean = GradientConfig(
        id = "ocean",
        name = "Ocean",
        colors = listOf(
            Color(0xFF00D2FF),
            Color(0xFF2563EB),
            Color(0xFF4F46E5)
        ),
        angleDegrees = 45f
    )

    // Preset 2: Aurora (Mint -> Turquoise -> Blue)
    val Aurora = GradientConfig(
        id = "aurora",
        name = "Aurora",
        colors = listOf(
            Color(0xFF10B981),
            Color(0xFF06B6D4),
            Color(0xFF3B82F6)
        ),
        angleDegrees = 45f
    )

    // Preset 3: Sunset (Orange -> Pink -> Violet)
    val Sunset = GradientConfig(
        id = "sunset",
        name = "Sunset",
        colors = listOf(
            Color(0xFFF97316),
            Color(0xFFEC4899),
            Color(0xFF8B5CF6)
        ),
        angleDegrees = 45f
    )

    // Preset 4: Neon (Green -> Cyan -> Blue)
    val Neon = GradientConfig(
        id = "neon",
        name = "Neon",
        colors = listOf(
            Color(0xFF22C55E),
            Color(0xFF06B6D4),
            Color(0xFF3B82F6)
        ),
        angleDegrees = 45f
    )

    // Preset 5: Fire (Orange -> Coral -> Pink)
    val Fire = GradientConfig(
        id = "fire",
        name = "Fire",
        colors = listOf(
            Color(0xFFEA580C),
            Color(0xFFF43F5E),
            Color(0xFFEC4899)
        ),
        angleDegrees = 45f
    )

    val AllPresets: List<GradientConfig> = listOf(
        Default,
        Ocean,
        Aurora,
        Sunset,
        Neon,
        Fire
    )

    fun findById(id: String): GradientConfig {
        return AllPresets.find { it.id == id } ?: Default
    }
}
