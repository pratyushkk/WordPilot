package com.wordpilot.assistant.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.overlay.FloatingSuggestionCard

@Composable
fun WordPilotKeyboardView(
    uiState: KeyboardUiState,
    onKeyClicked: (KeyAction) -> Unit,
    onAcceptCorrection: (Correction) -> Unit,
    onDismissCorrection: () -> Unit,
    onToggleExpandCard: () -> Unit,
    onAddToDictionary: (Correction) -> Unit,
    onIgnoreRule: (Correction) -> Unit,
    onWordSuggestionClicked: (String) -> Unit,
    onAssistantClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            // Floating Grammarly-style Suggestion Card or Prediction Bar
            AnimatedContent(
                targetState = uiState.activeCorrection,
                label = "SuggestionAnimation"
            ) { correction ->
                if (correction != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        FloatingSuggestionCard(
                            correction = correction,
                            isExpanded = uiState.isCorrectionCardExpanded,
                            onAccept = { onAcceptCorrection(correction) },
                            onDismiss = onDismissCorrection,
                            onToggleExpand = onToggleExpandCard,
                            onAddToDictionary = { onAddToDictionary(correction) },
                            onIgnoreRule = { onIgnoreRule(correction) }
                        )
                    }
                } else {
                    KeyboardSuggestionBar(
                        suggestions = uiState.wordSuggestions,
                        onSuggestionClicked = onWordSuggestionClicked,
                        onAssistantClicked = onAssistantClicked
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main key matrix
            when (uiState.mode) {
                KeyboardMode.QWERTY -> {
                    KeyboardRow(keys = KeyLayout.qwertyRow1, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.qwertyRow2, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.qwertyRow3, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.qwertyBottomRow, uiState = uiState, onKeyClicked = onKeyClicked)
                }
                KeyboardMode.NUMBERS -> {
                    KeyboardRow(keys = KeyLayout.numbersRow1, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.numbersRow2, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.numbersRow3, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.numbersBottomRow, uiState = uiState, onKeyClicked = onKeyClicked)
                }
                KeyboardMode.SYMBOLS -> {
                    KeyboardRow(keys = KeyLayout.numbersRow1, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.numbersRow2, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.numbersRow3, uiState = uiState, onKeyClicked = onKeyClicked)
                    KeyboardRow(keys = KeyLayout.numbersBottomRow, uiState = uiState, onKeyClicked = onKeyClicked)
                }
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    keys: List<KeyModel>,
    uiState: KeyboardUiState,
    onKeyClicked: (KeyAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (key in keys) {
            KeyView(
                key = key,
                uiState = uiState,
                onClick = { onKeyClicked(key.action) },
                modifier = Modifier.weight(key.weight)
            )
        }
    }
}

@Composable
private fun KeyView(
    key: KeyModel,
    uiState: KeyboardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isShifted = uiState.shiftState != ShiftState.OFF

    val backgroundColor = when {
        key.isSpecial -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (e: Exception) {
                    // Ignore haptic feedback error
                }
                onClick()
            },
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp),
        tonalElevation = if (key.isSpecial) 1.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val action = key.action) {
                is KeyAction.Character -> {
                    val label = if (isShifted) action.shifted else action.primary
                    Text(
                        text = label,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is KeyAction.Shift -> {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = "Shift",
                        modifier = Modifier.size(20.dp),
                        tint = when (uiState.shiftState) {
                            ShiftState.CAPS_LOCK -> MaterialTheme.colorScheme.primary
                            ShiftState.SHIFTED -> MaterialTheme.colorScheme.primary
                            ShiftState.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                is KeyAction.Backspace -> {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is KeyAction.Enter -> {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                is KeyAction.Space -> {
                    Text(
                        text = "English",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                is KeyAction.SwitchMode -> {
                    Text("?123", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                is KeyAction.SwitchToLetters -> {
                    Text("ABC", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                is KeyAction.SwitchToSymbols -> {
                    Text("=\\<", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                is KeyAction.WordPilotAssistant -> {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = "WordPilot",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
