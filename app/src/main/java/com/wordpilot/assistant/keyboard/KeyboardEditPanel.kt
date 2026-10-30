package com.wordpilot.assistant.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun KeyboardEditPanel(
    showNumberRow: Boolean,
    onToggleNumberRow: (Boolean) -> Unit,
    showKeyHints: Boolean,
    onToggleKeyHints: (Boolean) -> Unit,
    keyHeightDp: Int,
    onChangeKeyHeight: (Int) -> Unit,
    bottomPaddingDp: Int = 0,
    onChangeBottomPadding: (Int) -> Unit = {},
    sidePaddingDp: Int = 0,
    onChangeSidePadding: (Int) -> Unit = {},
    hapticEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit,
    onOpenResizeMode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentBlue = Color(0xFF8AB4F8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Edit Keyboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = onClose,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Done", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 1. Interactive Gboard Resize Feature Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF2C2D33) else Color(0xFFE3E8EE),
            border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenResizeMode() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentBlue.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = null,
                                tint = if (isDark) accentBlue else Color(0xFF1A73E8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Resize Keyboard (Gboard style)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Drag handles on screen to resize height & position",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onOpenResizeMode,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        "Resize",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF121316)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Keyboard Height Quick Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Height Preset",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            )
            Text(
                text = "${keyHeightDp} dp",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = accentBlue
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val presets = listOf(
                36 to "Extra Short",
                40 to "Short",
                46 to "Normal",
                52 to "Tall",
                58 to "Extra Tall"
            )
            for ((h, label) in presets) {
                val isSelected = keyHeightDp == h
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clickable { onChangeKeyHeight(h) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Height Slider
        Slider(
            value = keyHeightDp.toFloat(),
            onValueChange = { onChangeKeyHeight(it.roundToInt()) },
            valueRange = 32f..62f,
            steps = 29,
            modifier = Modifier.height(28.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Bottom Spacing (Elevation Chin)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bottom Elevation (Chin)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            )
            Text(
                text = "${bottomPaddingDp} dp",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = accentBlue
            )
        }
        Slider(
            value = bottomPaddingDp.toFloat(),
            onValueChange = { onChangeBottomPadding(it.roundToInt()) },
            valueRange = 0f..48f,
            steps = 23,
            modifier = Modifier.height(28.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 5. Number Row Toggle
        EditSwitchRow(
            title = "Number Row",
            subtitle = "Show dedicated row for numbers 1-0 above letters",
            checked = showNumberRow,
            onCheckedChange = onToggleNumberRow
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 6. Key Hints Toggle
        EditSwitchRow(
            title = "Key Hints (Long-press Symbols)",
            subtitle = "Display symbols & numbers on keys for quick long-press",
            checked = showKeyHints,
            onCheckedChange = onToggleKeyHints
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 7. Haptic Feedback Toggle
        EditSwitchRow(
            title = "Vibrate on Keypress",
            subtitle = "Subtle haptic feedback for every keystroke",
            checked = hapticEnabled,
            onCheckedChange = onToggleHaptic
        )
    }
}

@Composable
private fun EditSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

private fun Modifier.scale(scale: Float) = this.then(
    Modifier.size((50 * scale).dp, (30 * scale).dp)
)
