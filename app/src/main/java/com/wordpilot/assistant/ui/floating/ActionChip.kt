package com.wordpilot.assistant.ui.floating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.ui.theme.AssistantColors
import com.wordpilot.assistant.ui.theme.AssistantDimensions

@Composable
fun ActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    iconSize: Dp = AssistantDimensions.ChipIconSize,
    labelSize: TextUnit = AssistantDimensions.ChipLabelSize,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(AssistantDimensions.ChipCornerRadius)

    val backgroundColor = when {
        isSelected -> AssistantColors.AccentBlue.copy(alpha = 0.28f)
        else -> AssistantColors.ChipBackground
    }

    val borderColor = when {
        isSelected -> AssistantColors.AccentBlue.copy(alpha = 0.6f)
        else -> AssistantColors.ChipBorder
    }

    val contentColor = when {
        isSelected -> AssistantColors.AccentCyan
        !enabled -> AssistantColors.TextMuted
        else -> AssistantColors.ChipText
    }

    Surface(
        modifier = modifier
            .height(AssistantDimensions.ChipHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = AssistantDimensions.ChipPaddingHorizontal,
                    vertical = AssistantDimensions.ChipPaddingVertical
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )

            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.width(AssistantDimensions.ChipGap))
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = labelSize,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
