package com.wordpilot.assistant.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.keyboard.expanded.ExpandedWritingAssistant
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
    onCloseAssistant: () -> Unit = {},
    onApplyAssistantResult: (String) -> Unit = {},
    onRefreshAssistantText: () -> Unit = {},
    onUpdateAssistantSourceText: (String) -> Unit = {},
    onSelectAssistantStyle: (RewriteStyle) -> Unit = {},
    onCustomInstructionChanged: (String) -> Unit = {},
    onSelectAssistantEngine: (AIEngineType) -> Unit = {},
    onRetryAssistantRewrite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        AnimatedContent(
            targetState = uiState.mode == KeyboardMode.EXPANDED_ASSISTANT,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)))
            },
            label = "KeyboardModeTransition"
        ) { isExpanded ->
            if (isExpanded) {
                // =========================================================
                // STATE B: EXPANDED ASSISTANT PANEL
                // Occupies the keyboard's exact window area inside IME
                // =========================================================
                val session = uiState.assistantSession ?: AssistantSessionState()
                ExpandedWritingAssistant(
                    session = session,
                    onClose = onCloseAssistant,
                    onApply = onApplyAssistantResult,
                    onRefreshText = onRefreshAssistantText,
                    onUpdateSourceText = onUpdateAssistantSourceText,
                    onSelectStyle = onSelectAssistantStyle,
                    onCustomInstructionChanged = onCustomInstructionChanged,
                    onSelectEngine = onSelectAssistantEngine,
                    onRetry = onRetryAssistantRewrite
                )
            } else {
                // =========================================================
                // STATE A: COMPACT KEYBOARD WITH FLOATING SUGGESTION PILL
                // =========================================================
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
                                    onToggleExpand = onAssistantClicked, // Tapping the bubble transforms keyboard to expanded assistant!
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
                        KeyboardMode.EXPANDED_ASSISTANT -> {
                            // Handled above in animated branch
                        }
                    }
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

    val contentColor = when {
        key.isSpecial -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation = if (isPressed) 1.dp else 2.dp

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        tryAwaitRelease()
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(6.dp),
        color = if (isPressed) backgroundColor.copy(alpha = 0.7f) else backgroundColor,
        shadowElevation = elevation
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = contentColor
                    )
                }
                is KeyAction.Shift -> {
                    val iconTint = when (uiState.shiftState) {
                        ShiftState.OFF -> contentColor.copy(alpha = 0.6f)
                        ShiftState.SHIFTED -> MaterialTheme.colorScheme.primary
                        ShiftState.CAPS_LOCK -> MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Shift",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is KeyAction.Backspace -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is KeyAction.Enter -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is KeyAction.Space -> {
                    Text(
                        text = "space",
                        fontSize = 14.sp,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
                is KeyAction.SwitchMode -> {
                    Text(
                        text = "?123",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
                is KeyAction.SwitchToLetters -> {
                    Text(
                        text = "ABC",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
                is KeyAction.SwitchToSymbols -> {
                    Text(
                        text = "=\\<",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
                is KeyAction.WordPilotAssistant -> {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "WordPilot Assistant",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
