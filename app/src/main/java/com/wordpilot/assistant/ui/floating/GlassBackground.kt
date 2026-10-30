package com.wordpilot.assistant.ui.floating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wordpilot.assistant.ui.theme.AssistantColors
import com.wordpilot.assistant.ui.theme.AssistantDimensions

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = AssistantColors.SmokedGlassBase,
    opacity: Float = AssistantColors.DefaultGlassOpacity,
    cornerRadius: Dp = AssistantDimensions.DefaultCornerRadius,
    borderWidth: Dp = AssistantDimensions.DefaultBorderWidth,
    borderColor: Color = AssistantColors.GlassBorder,
    borderOpacity: Float = AssistantColors.DefaultBorderOpacity,
    highlightOpacity: Float = AssistantColors.DefaultHighlightOpacity,
    shadowElevation: Dp = AssistantDimensions.DefaultElevation,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveBackgroundColor = baseColor.copy(alpha = opacity.coerceIn(0.1f, 1f))
    val effectiveBorderColor = borderColor.copy(alpha = borderOpacity.coerceIn(0f, 1f))

    Surface(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            ),
        shape = shape,
        color = effectiveBackgroundColor,
        border = BorderStroke(borderWidth, effectiveBorderColor)
    ) {
        Box(
            modifier = Modifier.clip(shape)
        ) {
            // Subtle specular reflective highlight along the top edge
            if (highlightOpacity > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = highlightOpacity.coerceIn(0f, 1f)),
                                    Color.White.copy(alpha = (highlightOpacity * 0.8f).coerceIn(0f, 1f)),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            content()
        }
    }
}
