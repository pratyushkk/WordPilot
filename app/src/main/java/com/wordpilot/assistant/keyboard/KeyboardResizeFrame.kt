package com.wordpilot.assistant.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun KeyboardResizeFrame(
    uiState: KeyboardUiState,
    onSave: (heightDp: Int, bottomPaddingDp: Int, sidePaddingDp: Int) -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    var tempHeight by remember(uiState.keyHeightDp) {
        mutableFloatStateOf(uiState.keyHeightDp.toFloat())
    }
    var tempBottomPadding by remember(uiState.bottomPaddingDp) {
        mutableFloatStateOf(uiState.bottomPaddingDp.toFloat())
    }
    var tempSidePadding by remember(uiState.sidePaddingDp) {
        mutableFloatStateOf(uiState.sidePaddingDp.toFloat())
    }

    val accentBlue = Color(0xFF8AB4F8)
    val frameBackground = if (isDark) Color(0xFF1B1C20) else Color(0xFFE8EAED)
    val keyBg = if (isDark) Color(0xFF2C2D33) else Color(0xFFFFFFFF)
    val keySpecialBg = if (isDark) Color(0xFF222328) else Color(0xFFD2D6DB)
    val textPrimary = if (isDark) Color(0xFFF1F1F4) else Color(0xFF1F1F24)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(frameBackground)
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Gboard Floating Top Bar (Close, Dimension Badge, Reset, Save)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel / Close
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Resize",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Real-time Dimension Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentBlue.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Height: ${tempHeight.roundToInt()} dp • Bottom: ${tempBottomPadding.roundToInt()} dp",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) accentBlue else Color(0xFF1A73E8)
                    )
                }
            }

            // Right Actions: Reset & Save
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Reset to Default
                IconButton(
                    onClick = {
                        tempHeight = 46f
                        tempBottomPadding = 0f
                        tempSidePadding = 0f
                        onReset()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Size",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Done / Confirm Checkmark
                FilledIconButton(
                    onClick = {
                        onSave(
                            tempHeight.roundToInt(),
                            tempBottomPadding.roundToInt(),
                            tempSidePadding.roundToInt()
                        )
                    },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accentBlue,
                        contentColor = Color(0xFF121316)
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply Resize",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 2. Active Resizable Keyboard Container with Interactive Drag Handles
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = (tempSidePadding.dp + 4.dp),
                    end = (tempSidePadding.dp + 4.dp),
                    bottom = (tempBottomPadding.dp + 2.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Keyboard Bounding Border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        border = BorderStroke(2.dp, accentBlue),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .padding(top = 10.dp, bottom = 10.dp, start = 4.dp, end = 4.dp)
            ) {
                // Real-time Preview of Keyboard Rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val keyHeight = tempHeight.roundToInt()
                    if (uiState.showNumberRow) {
                        PreviewRow(
                            keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                            heightDp = (keyHeight * 0.8f).roundToInt(),
                            keyBg = keyBg,
                            textPrimary = textPrimary
                        )
                    }
                    PreviewRow(
                        keys = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                        heightDp = keyHeight,
                        keyBg = keyBg,
                        textPrimary = textPrimary
                    )
                    PreviewRow(
                        keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                        heightDp = keyHeight,
                        keyBg = keyBg,
                        textPrimary = textPrimary
                    )
                    PreviewRowWithSpecials(
                        keys = listOf("z", "x", "c", "v", "b", "n", "m"),
                        heightDp = keyHeight,
                        keyBg = keyBg,
                        keySpecialBg = keySpecialBg,
                        textPrimary = textPrimary,
                        accentBlue = accentBlue
                    )
                    PreviewBottomRow(
                        heightDp = keyHeight,
                        keyBg = keyBg,
                        keySpecialBg = keySpecialBg,
                        textPrimary = textPrimary,
                        accentBlue = accentBlue
                    )
                }
            }

            // Top Drag Handle (Pill with DragHandle)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-7).dp)
                    .size(width = 54.dp, height = 18.dp)
                    .background(accentBlue, RoundedCornerShape(9.dp))
                    .pointerInput(density) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = dragAmount.y / density.density
                            // Dragging up (negative y) increases height
                            tempHeight = (tempHeight - deltaDp).coerceIn(32f, 62f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to resize height",
                    tint = Color(0xFF121316),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom Drag Handle (Pill for Bottom Elevation)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 7.dp)
                    .size(width = 54.dp, height = 18.dp)
                    .background(accentBlue, RoundedCornerShape(9.dp))
                    .pointerInput(density) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = dragAmount.y / density.density
                            // Dragging up (negative y) increases bottom padding
                            tempBottomPadding = (tempBottomPadding - deltaDp).coerceIn(0f, 48f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to adjust bottom spacing",
                    tint = Color(0xFF121316),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Left Drag Handle (Pill for Side Padding)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-6).dp)
                    .size(width = 16.dp, height = 44.dp)
                    .background(accentBlue, RoundedCornerShape(8.dp))
                    .pointerInput(density) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = dragAmount.x / density.density
                            tempSidePadding = (tempSidePadding + deltaDp).coerceIn(0f, 36f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to adjust left width",
                    tint = Color(0xFF121316),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Right Drag Handle (Pill for Side Padding)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 6.dp)
                    .size(width = 16.dp, height = 44.dp)
                    .background(accentBlue, RoundedCornerShape(8.dp))
                    .pointerInput(density) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = dragAmount.x / density.density
                            tempSidePadding = (tempSidePadding - deltaDp).coerceIn(0f, 36f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to adjust right width",
                    tint = Color(0xFF121316),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(
    keys: List<String>,
    heightDp: Int,
    keyBg: Color,
    textPrimary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (key in keys) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = keyBg,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = key,
                        fontSize = (heightDp * 0.38f).sp,
                        fontWeight = FontWeight.Normal,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewRowWithSpecials(
    keys: List<String>,
    heightDp: Int,
    keyBg: Color,
    keySpecialBg: Color,
    textPrimary: Color,
    accentBlue: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shift Key
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = keySpecialBg,
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier.size((heightDp * 0.42f).dp)
                )
            }
        }

        // Letter Keys
        for (key in keys) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = keyBg,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = key,
                        fontSize = (heightDp * 0.38f).sp,
                        fontWeight = FontWeight.Normal,
                        color = textPrimary
                    )
                }
            }
        }

        // Backspace Key
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = keySpecialBg,
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier.size((heightDp * 0.42f).dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewBottomRow(
    heightDp: Int,
    keyBg: Color,
    keySpecialBg: Color,
    textPrimary: Color,
    accentBlue: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ?123
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = keySpecialBg,
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("?123", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textPrimary)
            }
        }

        // Emoji
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = keySpecialBg,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier.size((heightDp * 0.42f).dp)
                )
            }
        }

        // Space
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = keyBg,
            modifier = Modifier
                .weight(4.6f)
                .fillMaxHeight()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "English",
                    fontSize = 11.sp,
                    color = textPrimary.copy(alpha = 0.5f)
                )
            }
        }

        // Period
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = keyBg,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(".", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
        }

        // Enter
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentBlue,
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = null,
                    tint = Color(0xFF121316),
                    modifier = Modifier.size((heightDp * 0.42f).dp)
                )
            }
        }
    }
}
