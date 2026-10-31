package com.wordpilot.assistant.ui.floating

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.ui.settings.AppearanceSettings
import com.wordpilot.assistant.ui.theme.AssistantColors

data class ToolbarActionItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

object DefaultActionItems {
    val FixGrammar = ToolbarActionItem("fix_grammar", "Fix Grammar", Icons.Filled.Spellcheck)
    val Settings = ToolbarActionItem("settings", "Settings", Icons.Filled.Settings)

    val AllItems = listOf(
        FixGrammar,
        Settings
    )

    fun findById(id: String): ToolbarActionItem? = AllItems.find { it.id == id }
}

@Composable
fun ToolbarActionsContent(
    settings: AppearanceSettings,
    onActionClick: (String) -> Unit,
    onCollapse: () -> Unit,
    isExpandedToLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isExpandedToLeft) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Collapse",
                    tint = AssistantColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Horizontal scrollable actions list
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(settings.chipSpacingDp.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val actionIds = if (settings.activeQuickActions.isNotEmpty()) {
                settings.activeQuickActions.filter { it == "fix_grammar" || it == "settings" }.ifEmpty { listOf("fix_grammar", "settings") }
            } else {
                listOf("fix_grammar", "settings")
            }

            actionIds.forEach { actionId ->
                val item = DefaultActionItems.findById(actionId)
                if (item != null) {
                    ActionChip(
                        label = item.label,
                        icon = item.icon,
                        iconSize = settings.chipIconSizeDp.dp,
                        labelSize = settings.chipLabelSizeSp.sp,
                        onClick = { onActionClick(item.id) }
                    )
                }
            }
        }

        if (!isExpandedToLeft) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Collapse",
                    tint = AssistantColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandedToolbar(
    settings: AppearanceSettings,
    onActionClick: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    isExpandedToLeft: Boolean = false
) {
    GlassBackground(
        modifier = modifier
            .height(settings.toolbarHeightDp.dp)
            .widthIn(max = settings.toolbarMaxWidthDp.dp),
        opacity = settings.glassOpacity,
        baseColor = Color(settings.glassTintArgb),
        cornerRadius = settings.glassCornerRadiusDp.dp,
        borderWidth = settings.glassBorderWidthDp.dp,
        borderOpacity = settings.glassBorderOpacity,
        highlightOpacity = settings.glassHighlightOpacity,
        shadowElevation = settings.glassShadowElevationDp.dp
    ) {
        ToolbarActionsContent(
            settings = settings,
            onActionClick = onActionClick,
            onCollapse = onCollapse,
            isExpandedToLeft = isExpandedToLeft
        )
    }
}
