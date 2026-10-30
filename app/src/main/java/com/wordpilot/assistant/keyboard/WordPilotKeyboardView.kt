package com.wordpilot.assistant.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onCommitCharacter: (String) -> Unit,
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
    // Gboard Navigation & Editing
    onResizeClicked: () -> Unit = {},
    onOpenResizeMode: () -> Unit = {},
    onSaveResizeDimensions: (heightDp: Int, bottomPaddingDp: Int, sidePaddingDp: Int) -> Unit = { _, _, _ -> },
    onResetResizeDimensions: () -> Unit = {},
    onChangeBottomPadding: (Int) -> Unit = {},
    onChangeSidePadding: (Int) -> Unit = {},
    onTextEditingClicked: () -> Unit = {},
    onClipboardClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onHideKeyboard: () -> Unit = {},
    onMoveCursor: (Int) -> Unit = {},
    onJumpToStart: () -> Unit = {},
    onJumpToEnd: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    isSelecting: Boolean = false,
    onCut: () -> Unit = {},
    onCopy: () -> Unit = {},
    onPaste: () -> Unit = {},
    onToggleNumberRow: (Boolean) -> Unit = {},
    onToggleKeyHints: (Boolean) -> Unit = {},
    onChangeKeyHeight: (Int) -> Unit = {},
    onToggleHaptic: (Boolean) -> Unit = {},
    onPasteClip: (String) -> Unit = {},
    onClearClips: () -> Unit = {},
    onEmojiClicked: (String) -> Unit = {},
    onClosePanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val keyboardBackground = if (isDark) Color(0xFF1E1F24) else Color(0xFFECEFF1)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = keyboardBackground
    ) {
        AnimatedContent(
            targetState = uiState.mode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(200)))
                    .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutVertically(animationSpec = tween(160)))
            },
            label = "KeyboardModeContent"
        ) { mode ->
            when (mode) {
                KeyboardMode.EXPANDED_ASSISTANT -> {
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
                }
                KeyboardMode.TEXT_EDITING -> {
                    TextEditingPanel(
                        onMoveCursor = onMoveCursor,
                        onJumpToStart = onJumpToStart,
                        onJumpToEnd = onJumpToEnd,
                        onSelectAll = onSelectAll,
                        onToggleSelect = onToggleSelect,
                        isSelecting = isSelecting,
                        onCut = onCut,
                        onCopy = onCopy,
                        onPaste = onPaste,
                        onBackspace = { onKeyClicked(KeyAction.Backspace) },
                        onEnter = { onKeyClicked(KeyAction.Enter) },
                        onClose = onClosePanel
                    )
                }
                KeyboardMode.CLIPBOARD -> {
                    ClipboardPanel(
                        clips = uiState.clipboardHistory,
                        onPasteClip = onPasteClip,
                        onClearClips = onClearClips,
                        onClose = onClosePanel
                    )
                }
                KeyboardMode.EMOJI -> {
                    EmojiPanel(
                        onEmojiClicked = onEmojiClicked,
                        onBackspace = { onKeyClicked(KeyAction.Backspace) },
                        onClose = onClosePanel
                    )
                }
                KeyboardMode.RESIZE -> {
                    KeyboardResizeFrame(
                        uiState = uiState,
                        onSave = onSaveResizeDimensions,
                        onCancel = onClosePanel,
                        onReset = onResetResizeDimensions
                    )
                }
                KeyboardMode.EDIT_SETTINGS -> {
                    KeyboardEditPanel(
                        showNumberRow = uiState.showNumberRow,
                        onToggleNumberRow = onToggleNumberRow,
                        showKeyHints = uiState.showKeyHints,
                        onToggleKeyHints = onToggleKeyHints,
                        keyHeightDp = uiState.keyHeightDp,
                        onChangeKeyHeight = onChangeKeyHeight,
                        bottomPaddingDp = uiState.bottomPaddingDp,
                        onChangeBottomPadding = onChangeBottomPadding,
                        sidePaddingDp = uiState.sidePaddingDp,
                        onChangeSidePadding = onChangeSidePadding,
                        hapticEnabled = uiState.hapticEnabled,
                        onToggleHaptic = onToggleHaptic,
                        onOpenResizeMode = onOpenResizeMode,
                        onClose = onClosePanel
                    )
                }
                KeyboardMode.QWERTY,
                KeyboardMode.NUMBERS,
                KeyboardMode.SYMBOLS -> {
                    val bottomMargin = if (uiState.bottomPaddingDp > 0) uiState.bottomPaddingDp.dp else 8.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = uiState.sidePaddingDp.dp,
                                end = uiState.sidePaddingDp.dp,
                                bottom = bottomMargin
                            )
                    ) {
                        // 1. Gboard Toolbar (or Suggestion Card)
                        if (uiState.activeCorrection != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                FloatingSuggestionCard(
                                    correction = uiState.activeCorrection,
                                    isExpanded = uiState.isCorrectionCardExpanded,
                                    onAccept = { onAcceptCorrection(uiState.activeCorrection) },
                                    onDismiss = onDismissCorrection,
                                    onToggleExpand = onAssistantClicked,
                                    onAddToDictionary = { onAddToDictionary(uiState.activeCorrection) },
                                    onIgnoreRule = { onIgnoreRule(uiState.activeCorrection) }
                                )
                            }
                        } else {
                            GboardToolbar(
                                suggestions = uiState.wordSuggestions,
                                onSuggestionClicked = onWordSuggestionClicked,
                                onAssistantClicked = onAssistantClicked,
                                onResizeClicked = onResizeClicked,
                                onTextEditingClicked = onTextEditingClicked,
                                onClipboardClicked = onClipboardClicked,
                                onSettingsClicked = onSettingsClicked,
                                onHideKeyboard = onHideKeyboard
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // 2. Main Key Rows (exact Gboard layout)
                        when (mode) {
                            KeyboardMode.QWERTY -> {
                                if (uiState.showNumberRow) {
                                    KeyboardRow(
                                        keys = KeyLayout.numberRow,
                                        uiState = uiState,
                                        heightDp = (uiState.keyHeightDp * 0.82f).toInt(),
                                        onKeyClicked = onKeyClicked,
                                        onCommitCharacter = onCommitCharacter
                                    )
                                }
                                KeyboardRow(keys = KeyLayout.qwertyRow1, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.qwertyRow2, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.qwertyRow3, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.qwertyBottomRow, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                            }
                            KeyboardMode.NUMBERS -> {
                                KeyboardRow(keys = KeyLayout.numbersRow1, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.numbersRow2, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.numbersRow3, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.numbersBottomRow, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                            }
                            KeyboardMode.SYMBOLS -> {
                                KeyboardRow(keys = KeyLayout.symbolsRow1, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.symbolsRow2, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.symbolsRow3, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                                KeyboardRow(keys = KeyLayout.symbolsBottomRow, uiState = uiState, heightDp = uiState.keyHeightDp, onKeyClicked = onKeyClicked, onCommitCharacter = onCommitCharacter)
                            }
                            else -> {}
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
    heightDp: Int,
    onKeyClicked: (KeyAction) -> Unit,
    onCommitCharacter: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (key in keys) {
            GboardKeyView(
                key = key,
                uiState = uiState,
                onClick = { onKeyClicked(key.action) },
                onKeyAction = onKeyClicked,
                onLongPress = {
                    when (val action = key.action) {
                        is KeyAction.Character -> {
                            action.hint?.let { onCommitCharacter(it) } ?: onKeyClicked(action)
                        }
                        is KeyAction.Emoji -> onKeyClicked(KeyAction.Emoji)
                        else -> onKeyClicked(action)
                    }
                },
                modifier = Modifier.weight(key.weight)
            )
        }
    }
}

@Composable
private fun GboardKeyView(
    key: KeyModel,
    uiState: KeyboardUiState,
    onClick: () -> Unit,
    onKeyAction: (KeyAction) -> Unit = {},
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val isShifted = uiState.shiftState != ShiftState.OFF
    val isDark = isSystemInDarkTheme()

    // Gboard Color Palette
    val normalKeyBg = if (isDark) Color(0xFF2C2D33) else Color(0xFFFFFFFF)
    val specialKeyBg = if (isDark) Color(0xFF222328) else Color(0xFFDCE1E6)
    val actionKeyBg = if (isDark) Color(0xFF8AB4F8) else Color(0xFF1976D2)
    val normalContentColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1F1F1F)
    val actionContentColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFFFFFFF)
    val hintColor = if (isDark) Color(0xFF9AA0A6) else Color(0xFF70757A)

    val backgroundColor = when {
        key.isAction -> actionKeyBg
        key.isSpecial -> specialKeyBg
        else -> normalKeyBg
    }

    val contentColor = when {
        key.isAction -> actionContentColor
        key.isSpecial -> if (isDark) Color(0xFFE8EAED) else Color(0xFF3C4043)
        else -> normalContentColor
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(key.action) {
                if (key.action is KeyAction.Backspace) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        if (uiState.hapticEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        onClick()

                        var isHolding = true
                        val job = coroutineScope.launch {
                            delay(350L)
                            var repeatCount = 0
                            while (isHolding) {
                                repeatCount++
                                if (repeatCount > 7) {
                                    // Accelerated delete: delete whole words!
                                    onKeyAction(KeyAction.DeleteWord)
                                    if (uiState.hapticEnabled) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    delay(140L)
                                } else {
                                    // Rapid character-by-character deletion
                                    onClick()
                                    if (uiState.hapticEnabled) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    delay(55L)
                                }
                            }
                        }

                        waitForUpOrCancellation()
                        isHolding = false
                        job.cancel()
                    }
                } else {
                    detectTapGestures(
                        onPress = {
                            if (uiState.hapticEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            tryAwaitRelease()
                        },
                        onTap = { onClick() },
                        onLongPress = {
                            if (uiState.hapticEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            onLongPress()
                        }
                    )
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = if (isPressed) backgroundColor.copy(alpha = 0.65f) else backgroundColor,
        shadowElevation = if (isPressed) 0.5.dp else 1.5.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val action = key.action) {
                is KeyAction.Character -> {
                    val label = if (isShifted) action.shifted else action.primary

                    // Secondary Hint in top-right corner (Gboard style)
                    if (uiState.showKeyHints && action.hint != null) {
                        Text(
                            text = action.hint,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            color = hintColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 4.dp, top = 2.dp)
                        )
                    }

                    // Main Character
                    Text(
                        text = label,
                        fontSize = if (label.length > 1) 16.sp else 21.sp,
                        fontWeight = FontWeight.Normal,
                        color = contentColor
                    )
                }
                is KeyAction.Shift -> {
                    val iconTint = when (uiState.shiftState) {
                        ShiftState.OFF -> contentColor.copy(alpha = 0.7f)
                        ShiftState.SHIFTED -> Color(0xFF8AB4F8)
                        ShiftState.CAPS_LOCK -> Color(0xFF8AB4F8)
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Shift",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is KeyAction.Backspace,
                is KeyAction.DeleteWord -> {
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
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                is KeyAction.Space -> {
                    Text(
                        text = "English",
                        fontSize = 13.sp,
                        color = contentColor.copy(alpha = 0.55f)
                    )
                }
                is KeyAction.Emoji -> {
                    Icon(
                        imageVector = Icons.Outlined.Mood,
                        contentDescription = "Emoji",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is KeyAction.SwitchMode -> {
                    Text(
                        text = "?123",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
                is KeyAction.SwitchToLetters -> {
                    Text(
                        text = "ABC",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
                is KeyAction.SwitchToSymbols -> {
                    Text(
                        text = "=\\<",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
                is KeyAction.WordPilotAssistant -> {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Assistant",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(20.dp)
                    )
                }
                is KeyAction.TextEditing -> {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                is KeyAction.Clipboard -> {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Clipboard",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                is KeyAction.EditKeyboard -> {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Edit Keyboard",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                is KeyAction.ResizeKeyboard -> {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Resize Keyboard",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
