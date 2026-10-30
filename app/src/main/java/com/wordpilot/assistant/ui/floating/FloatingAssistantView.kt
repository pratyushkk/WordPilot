package com.wordpilot.assistant.ui.floating

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.ui.settings.AppearanceSettings
import com.wordpilot.assistant.ui.theme.AssistantColors

enum class FloatingUiState {
    COLLAPSED,
    EXPANDED,
    MORE_OPTIONS,
    PREVIEW
}

@Composable
fun FloatingAssistantView(
    settings: AppearanceSettings,
    uiState: FloatingUiState,
    isNearRightEdge: Boolean,
    errorCount: Int,
    isChecking: Boolean,
    activeCorrection: Correction?,
    previewOriginalText: String,
    previewImprovedText: String,
    isPreviewLoading: Boolean,
    previewErrorMessage: String?,
    canUndo: Boolean,
    currentTone: String,
    onBubbleClick: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onActionClick: (String) -> Unit,
    onCustomPromptSubmit: (String) -> Unit,
    onSelectTone: (String) -> Unit,
    onReplace: () -> Unit,
    onInsert: () -> Unit,
    onCopy: () -> Unit,
    onCancelPreview: () -> Unit,
    onUndo: () -> Unit,
    onOpenSettings: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (activeCorrection?.category) {
        CorrectionCategory.SPELLING -> AssistantColors.BadgeSpelling
        CorrectionCategory.GRAMMAR -> AssistantColors.BadgeGrammar
        CorrectionCategory.PUNCTUATION -> AssistantColors.BadgePunctuation
        CorrectionCategory.STYLE -> AssistantColors.BadgeStyle
        CorrectionCategory.CAPITALIZATION -> AssistantColors.BadgeCapitalization
        null -> AssistantColors.BadgeGrammar
    }

    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = if (isNearRightEdge) Alignment.End else Alignment.Start
    ) {
        // Upper Panels: More Options or Preview appear above/beside the toolbar if active
        AnimatedVisibility(
            visible = uiState == FloatingUiState.MORE_OPTIONS,
            enter = fadeIn(tween(200)) + expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(tween(150)) + shrinkVertically()
        ) {
            Box(modifier = Modifier.padding(bottom = 6.dp)) {
                MoreOptionsPanel(
                    settings = settings,
                    onActionClick = onActionClick,
                    onCustomPromptSubmit = onCustomPromptSubmit,
                    onOpenSettings = onOpenSettings,
                    onBack = { onActionClick("back_to_expanded") }
                )
            }
        }

        AnimatedVisibility(
            visible = uiState == FloatingUiState.PREVIEW,
            enter = fadeIn(tween(200)) + expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(tween(150)) + shrinkVertically()
        ) {
            Box(modifier = Modifier.padding(bottom = 6.dp)) {
                SuggestionPreview(
                    originalText = previewOriginalText,
                    improvedText = previewImprovedText,
                    isLoading = isPreviewLoading,
                    errorMessage = previewErrorMessage,
                    canUndo = canUndo,
                    currentTone = currentTone,
                    settings = settings,
                    onReplace = onReplace,
                    onInsert = onInsert,
                    onCopy = onCopy,
                    onCancel = onCancelPreview,
                    onUndo = onUndo,
                    onSelectTone = onSelectTone
                )
            }
        }

        // Main Horizontal Row: Bubble Anchor + Horizontally Expanding Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isNearRightEdge) Arrangement.End else Arrangement.Start,
            modifier = Modifier.wrapContentSize()
        ) {
            if (isNearRightEdge) {
                // Expanding to the left: Toolbar on left, Bubble on right
                AnimatedVisibility(
                    visible = uiState == FloatingUiState.EXPANDED,
                    enter = expandHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        expandFrom = Alignment.End
                    ) + fadeIn(tween(180)),
                    exit = shrinkHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        shrinkTowards = Alignment.End
                    ) + fadeOut(tween(150))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExpandedToolbar(
                            settings = settings,
                            onActionClick = onActionClick,
                            onCollapse = onCollapse,
                            isExpandedToLeft = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                FloatingBubble(
                    settings = settings,
                    errorCount = errorCount,
                    isChecking = isChecking,
                    badgeColor = badgeColor,
                    onClick = onBubbleClick,
                    onDrag = onDrag
                )
            } else {
                // Expanding to the right: Bubble on left, Toolbar on right
                FloatingBubble(
                    settings = settings,
                    errorCount = errorCount,
                    isChecking = isChecking,
                    badgeColor = badgeColor,
                    onClick = onBubbleClick,
                    onDrag = onDrag
                )

                AnimatedVisibility(
                    visible = uiState == FloatingUiState.EXPANDED,
                    enter = expandHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        expandFrom = Alignment.Start
                    ) + fadeIn(tween(180)),
                    exit = shrinkHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        shrinkTowards = Alignment.Start
                    ) + fadeOut(tween(150))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(6.dp))
                        ExpandedToolbar(
                            settings = settings,
                            onActionClick = onActionClick,
                            onCollapse = onCollapse,
                            isExpandedToLeft = false
                        )
                    }
                }
            }
        }
    }
}
