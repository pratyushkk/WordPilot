package com.wordpilot.assistant.ui.floating

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.ui.settings.AppearanceSettings

data class ToolbarActionItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

object DefaultActionItems {
    val FixGrammar = ToolbarActionItem("fix_grammar", "Fix Grammar", Icons.Filled.Spellcheck)
    val Rephrase = ToolbarActionItem("rephrase", "Rephrase", Icons.Filled.AutoFixHigh)
    val Professional = ToolbarActionItem("professional", "Professional", Icons.Filled.BusinessCenter)
    val Shorten = ToolbarActionItem("shorten", "Shorten", Icons.Filled.Compress)
    val More = ToolbarActionItem("more", "More", Icons.Filled.MoreHoriz)

    // Secondary items
    val Expand = ToolbarActionItem("expand", "Expand", Icons.Filled.OpenInFull)
    val Casual = ToolbarActionItem("casual", "Casual", Icons.Filled.SentimentSatisfied)
    val Formal = ToolbarActionItem("formal", "Formal", Icons.Filled.AccountBalance)
    val Friendly = ToolbarActionItem("friendly", "Friendly", Icons.Filled.FavoriteBorder)
    val Summarize = ToolbarActionItem("summarize", "Summarize", Icons.Filled.Summarize)
    val Translate = ToolbarActionItem("translate", "Translate", Icons.Filled.Translate)
    val Clarity = ToolbarActionItem("clarity", "Improve Clarity", Icons.Filled.Lightbulb)
    val Punctuation = ToolbarActionItem("punctuation", "Fix Punctuation", Icons.Filled.EditNote)
    val CustomPrompt = ToolbarActionItem("custom_prompt", "Custom Prompt", Icons.Filled.Psychology)

    val AllItems = listOf(
        FixGrammar,
        Rephrase,
        Professional,
        Shorten,
        More,
        Expand,
        Casual,
        Formal,
        Friendly,
        Summarize,
        Translate,
        Clarity,
        Punctuation,
        CustomPrompt
    )

    fun findById(id: String): ToolbarActionItem? = AllItems.find { it.id == id }
}

@Composable
fun ExpandedToolbar(
    settings: AppearanceSettings,
    onActionClick: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    isExpandedToLeft: Boolean = false
) {
    val scrollState = rememberScrollState()

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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Horizontal scrollable actions list
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(settings.chipSpacingDp.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val actionIds = if (settings.activeQuickActions.isNotEmpty()) {
                    settings.activeQuickActions
                } else {
                    listOf("fix_grammar", "rephrase", "professional", "shorten", "more")
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
        }
    }
}
