package com.wordpilot.assistant.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextEditingPanel(
    onMoveCursor: (direction: Int) -> Unit, // -1: Left, 1: Right, -2: Up, 2: Down
    onJumpToStart: () -> Unit,
    onJumpToEnd: () -> Unit,
    onSelectAll: () -> Unit,
    onToggleSelect: () -> Unit,
    isSelecting: Boolean,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val keyBg = if (isDark) Color(0xFF2E2F35) else Color(0xFFFFFFFF)
    val actionBg = if (isDark) Color(0xFF383A42) else Color(0xFFE2E6EA)
    val primaryBlue = Color(0xFF1976D2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(265.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to ABC",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Text Editing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditActionButton(
                    text = if (isSelecting) "Selecting..." else "Select",
                    isSelected = isSelecting,
                    onClick = onToggleSelect
                )
                EditActionButton(
                    text = "Select All",
                    onClick = onSelectAll
                )
            }
        }

        // 2. Middle Row: D-Pad & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action Column: Cut, Copy, Paste
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                EditButtonWithIcon(icon = Icons.Default.ContentCut, label = "Cut", onClick = onCut, bg = actionBg)
                EditButtonWithIcon(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopy, bg = actionBg)
                EditButtonWithIcon(icon = Icons.Default.ContentPaste, label = "Paste", onClick = onPaste, bg = actionBg)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Center: D-Pad (Up, Down, Left, Right, Home, End)
            Box(
                modifier = Modifier
                    .size(175.dp),
                contentAlignment = Alignment.Center
            ) {
                // Up Arrow
                DPadArrow(
                    icon = Icons.Default.KeyboardArrowUp,
                    modifier = Modifier.align(Alignment.TopCenter),
                    onClick = { onMoveCursor(-2) },
                    bg = keyBg
                )

                // Down Arrow
                DPadArrow(
                    icon = Icons.Default.KeyboardArrowDown,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onClick = { onMoveCursor(2) },
                    bg = keyBg
                )

                // Left Arrow
                DPadArrow(
                    icon = Icons.Default.KeyboardArrowLeft,
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = { onMoveCursor(-1) },
                    bg = keyBg
                )

                // Right Arrow
                DPadArrow(
                    icon = Icons.Default.KeyboardArrowRight,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { onMoveCursor(1) },
                    bg = keyBg
                )

                // Center Jump Buttons
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = actionBg,
                        modifier = Modifier
                            .clickable { onJumpToStart() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("Start", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = actionBg,
                        modifier = Modifier
                            .clickable { onJumpToEnd() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("End", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Action Column: Backspace & Enter
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                EditButtonWithIcon(icon = Icons.AutoMirrored.Filled.Backspace, label = "Delete", onClick = onBackspace, bg = actionBg)
                EditButtonWithIcon(icon = Icons.Default.KeyboardReturn, label = "Enter", onClick = onEnter, bg = primaryBlue, isPrimary = true)
            }
        }
    }
}

@Composable
private fun DPadArrow(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    bg: Color
) {
    Surface(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape),
        shape = CircleShape,
        color = bg,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun EditActionButton(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun EditButtonWithIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    bg: Color,
    isPrimary: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
