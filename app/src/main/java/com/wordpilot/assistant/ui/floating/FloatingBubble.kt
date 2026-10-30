package com.wordpilot.assistant.ui.floating

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.ui.settings.AppearanceSettings
import com.wordpilot.assistant.ui.theme.AssistantColors
import kotlinx.coroutines.delay

@Composable
fun FloatingBubble(
    settings: AppearanceSettings,
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    errorCount: Int = 0,
    isChecking: Boolean = false,
    badgeColor: Color = AssistantColors.BadgeGrammar
) {
    // Auto-dimming state tracking
    var isDimmed by remember { mutableStateOf(false) }
    var lastInteractionTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(settings.autoDimEnabled, settings.autoDimDelaySeconds, lastInteractionTimestamp) {
        if (settings.autoDimEnabled && settings.autoDimDelaySeconds > 0) {
            isDimmed = false
            delay(settings.autoDimDelaySeconds * 1000L)
            isDimmed = true
        } else {
            isDimmed = false
        }
    }

    val targetDimMultiplier = if (isDimmed) settings.autoDimOpacity.coerceIn(0.1f, 1f) else 1.0f
    val animatedMultiplier by animateFloatAsState(
        targetValue = targetDimMultiplier,
        animationSpec = tween(durationMillis = 350),
        label = "autoDimAnimation"
    )

    val sizeDp = settings.bubbleSizeDp.dp
    val shape = settings.bubbleShape.toShape(settings.bubbleSizeDp, settings.bubbleCornerRadiusDp)

    // Independent layer alphas modulated by auto-dim
    val effectiveBgAlpha = (settings.backgroundOpacity * animatedMultiplier).coerceIn(0.05f, 1f)
    val effectiveIconAlpha = (settings.iconOpacity * animatedMultiplier).coerceIn(0.05f, 1f)
    val effectiveBorderAlpha = (settings.borderOpacity * animatedMultiplier).coerceIn(0f, 1f)
    val effectiveShadowElevation = (settings.shadowIntensity * 12f * animatedMultiplier).dp

    val gradient = settings.getEffectiveGradient()

    Box(
        modifier = modifier
            .size(sizeDp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        lastInteractionTimestamp = System.currentTimeMillis()
                        isDimmed = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        lastInteractionTimestamp = System.currentTimeMillis()
                        isDimmed = false
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    lastInteractionTimestamp = System.currentTimeMillis()
                    isDimmed = false
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Main bubble surface with shadow and gradient
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = effectiveShadowElevation,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.4f * settings.shadowIntensity),
                    spotColor = Color.Black.copy(alpha = 0.6f * settings.shadowIntensity)
                ),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(
                settings.borderWidthDp.dp,
                Color.White.copy(alpha = effectiveBorderAlpha)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient.createBrush(), alpha = effectiveBgAlpha),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(settings.iconSizeDp.dp),
                        color = Color.White.copy(alpha = effectiveIconAlpha),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "WordPilot Assistant",
                        tint = Color.White.copy(alpha = effectiveIconAlpha),
                        modifier = Modifier.size(settings.iconSizeDp.dp)
                    )
                }
            }
        }

        // Circular error counter badge on top-right corner
        if (errorCount > 0 && !isChecking) {
            val badgeSize = (sizeDp * 0.38f).coerceIn(16.dp, 24.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(badgeSize)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (errorCount > 9) "9+" else errorCount.toString(),
                    color = Color.White,
                    fontSize = (badgeSize.value * 0.58f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
