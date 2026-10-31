package com.wordpilot.assistant.ui.settings

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.ui.floating.DefaultActionItems
import com.wordpilot.assistant.ui.floating.FloatingAssistantView
import com.wordpilot.assistant.ui.floating.FloatingUiState
import com.wordpilot.assistant.ui.theme.AssistantColors
import com.wordpilot.assistant.ui.theme.GradientConfig
import com.wordpilot.assistant.ui.theme.GradientPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(AppearanceSettings.load(context)) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bubble, 1: Gradient, 2: Glass, 3: Toolbar, 4: Actions

    // Preview state inside studio
    var previewUiState by remember { mutableStateOf(FloatingUiState.EXPANDED) }

    fun updateAndSave(newSettings: AppearanceSettings) {
        settings = newSettings
        AppearanceSettings.save(context, newSettings)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance & Layout Studio", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val defaultSettings = AppearanceSettings()
                            updateAndSave(defaultSettings)
                        }
                    ) {
                        Text("Reset All", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // =========================================================================
            // 1. STICKY LIVE INTERACTIVE PREVIEW
            // =========================================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Mode toggle chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PreviewModeChip("Collapsed", previewUiState == FloatingUiState.COLLAPSED) {
                                previewUiState = FloatingUiState.COLLAPSED
                            }
                            PreviewModeChip("Expanded", previewUiState == FloatingUiState.EXPANDED) {
                                previewUiState = FloatingUiState.EXPANDED
                            }
                            PreviewModeChip("Preview", previewUiState == FloatingUiState.PREVIEW) {
                                previewUiState = FloatingUiState.PREVIEW
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulated Screen Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F1115))
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        FloatingAssistantView(
                            settings = settings,
                            uiState = previewUiState,
                            isNearRightEdge = false,
                            errorCount = 1,
                            isChecking = false,
                            activeCorrection = null,
                            previewOriginalText = "She have went to a apple store.",
                            previewImprovedText = "She has gone to an apple store.",
                            isPreviewLoading = false,
                            previewErrorMessage = null,
                            canUndo = true,
                            currentTone = "Default",
                            onBubbleClick = {
                                previewUiState = if (previewUiState == FloatingUiState.COLLAPSED) FloatingUiState.PREVIEW else FloatingUiState.COLLAPSED
                            },
                            onDrag = { _, _ -> },
                            onActionClick = { actionId ->
                                when (actionId) {
                                    "more" -> previewUiState = FloatingUiState.MORE_OPTIONS
                                    "back_to_expanded" -> previewUiState = FloatingUiState.COLLAPSED
                                    else -> previewUiState = FloatingUiState.PREVIEW
                                }
                            },
                            onCustomPromptSubmit = {},
                            onSelectTone = {},
                            onReplace = {},
                            onInsert = {},
                            onCopy = {},
                            onCancelPreview = { previewUiState = FloatingUiState.COLLAPSED },
                            onUndo = {},
                            onOpenSettings = {},
                            onCollapse = { previewUiState = FloatingUiState.COLLAPSED }
                        )
                    }
                }
            }

            // =========================================================================
            // 2. CATEGORY TABS
            // =========================================================================
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bubble", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Gradient", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Glass", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Toolbar", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Actions", fontSize = 12.sp) }
                )
            }

            // =========================================================================
            // 3. TAB CONTENT
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> BubbleSettingsSection(settings) { updateAndSave(it) }
                    1 -> GradientSettingsSection(settings) { updateAndSave(it) }
                    2 -> GlassSettingsSection(settings) { updateAndSave(it) }
                    3 -> ToolbarSettingsSection(settings) { updateAndSave(it) }
                    4 -> ActionsSettingsSection(settings) { updateAndSave(it) }
                }
            }
        }
    }
}

@Composable
private fun PreviewModeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.height(24.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// =============================================================================
// SUB-SECTIONS
// =============================================================================

@Composable
private fun BubbleSettingsSection(
    settings: AppearanceSettings,
    onUpdate: (AppearanceSettings) -> Unit
) {
    Text("Bubble Shape", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BubbleShape.values().forEach { shape ->
            val isSelected = settings.bubbleShape == shape
            OutlinedButton(
                onClick = { onUpdate(settings.copy(bubbleShape = shape)) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text(shape.displayName, fontSize = 11.sp, maxLines = 1)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    StudioSlider(
        label = "Bubble Size",
        value = settings.bubbleSizeDp,
        range = 32f..72f,
        suffix = "dp"
    ) { onUpdate(settings.copy(bubbleSizeDp = it)) }

    if (settings.bubbleShape == BubbleShape.CUSTOM) {
        StudioSlider(
            label = "Custom Corner Radius",
            value = settings.bubbleCornerRadiusDp,
            range = 0f..36f,
            suffix = "dp"
        ) { onUpdate(settings.copy(bubbleCornerRadiusDp = it)) }
    }

    StudioSlider(
        label = "Icon Size",
        value = settings.iconSizeDp,
        range = 16f..36f,
        suffix = "dp"
    ) { onUpdate(settings.copy(iconSizeDp = it)) }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

    Text("Independent Layer Opacity Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(6.dp))

    StudioSlider(
        label = "Gradient Background Opacity",
        value = settings.backgroundOpacity * 100f,
        range = 10f..100f,
        suffix = "%"
    ) { onUpdate(settings.copy(backgroundOpacity = it / 100f)) }

    StudioSlider(
        label = "Icon Opacity",
        value = settings.iconOpacity * 100f,
        range = 10f..100f,
        suffix = "%"
    ) { onUpdate(settings.copy(iconOpacity = it / 100f)) }

    StudioSlider(
        label = "Border Opacity",
        value = settings.borderOpacity * 100f,
        range = 0f..100f,
        suffix = "%"
    ) { onUpdate(settings.copy(borderOpacity = it / 100f)) }

    StudioSlider(
        label = "Border Thickness",
        value = settings.borderWidthDp,
        range = 0.5f..4f,
        suffix = "dp"
    ) { onUpdate(settings.copy(borderWidthDp = it)) }

    StudioSlider(
        label = "Shadow Intensity",
        value = settings.shadowIntensity * 100f,
        range = 0f..100f,
        suffix = "%"
    ) { onUpdate(settings.copy(shadowIntensity = it / 100f)) }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

    Text("Automatic Dimming (Idle Transparency)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Enable Auto Dim", fontSize = 13.sp)
        Switch(
            checked = settings.autoDimEnabled,
            onCheckedChange = { onUpdate(settings.copy(autoDimEnabled = it)) }
        )
    }

    if (settings.autoDimEnabled) {
        StudioSlider(
            label = "Idle Delay Before Dimming",
            value = settings.autoDimDelaySeconds.toFloat(),
            range = 2f..30f,
            suffix = "s"
        ) { onUpdate(settings.copy(autoDimDelaySeconds = it.toInt())) }

        StudioSlider(
            label = "Dimmed Opacity",
            value = settings.autoDimOpacity * 100f,
            range = 10f..80f,
            suffix = "%"
        ) { onUpdate(settings.copy(autoDimOpacity = it / 100f)) }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

    Text("Visibility Behavior", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text("Hide When Keyboard Closed", fontSize = 13.sp)
            Text(
                "Only display the floating bubble while typing. Turn off to keep it docked on screen when active.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = settings.autoHideWhenKeyboardClosed,
            onCheckedChange = { onUpdate(settings.copy(autoHideWhenKeyboardClosed = it)) }
        )
    }
}

@Composable
private fun GradientSettingsSection(
    settings: AppearanceSettings,
    onUpdate: (AppearanceSettings) -> Unit
) {
    Text("Gradient Presets", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GradientPresets.AllPresets.forEach { preset ->
            val isSelected = settings.gradientId == preset.id
            Surface(
                onClick = { onUpdate(settings.copy(gradientId = preset.id)) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 1.5.dp else 0.8.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    // Gradient preview swatch
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(preset.createBrush())
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    StudioSlider(
        label = "Gradient Direction (Angle)",
        value = settings.gradientAngle,
        range = 0f..360f,
        suffix = "°"
    ) { onUpdate(settings.copy(gradientAngle = it)) }
}

@Composable
private fun GlassSettingsSection(
    settings: AppearanceSettings,
    onUpdate: (AppearanceSettings) -> Unit
) {
    Text("Dark Smoked Glass Properties", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(8.dp))

    StudioSlider(
        label = "Glass Transparency (Opacity)",
        value = settings.glassOpacity * 100f,
        range = 30f..100f,
        suffix = "%"
    ) { onUpdate(settings.copy(glassOpacity = it / 100f)) }

    StudioSlider(
        label = "Corner Radius",
        value = settings.glassCornerRadiusDp,
        range = 12f..36f,
        suffix = "dp"
    ) { onUpdate(settings.copy(glassCornerRadiusDp = it)) }

    StudioSlider(
        label = "Border Opacity",
        value = settings.glassBorderOpacity * 100f,
        range = 0f..60f,
        suffix = "%"
    ) { onUpdate(settings.copy(glassBorderOpacity = it / 100f)) }

    StudioSlider(
        label = "Border Width",
        value = settings.glassBorderWidthDp,
        range = 0.5f..3f,
        suffix = "dp"
    ) { onUpdate(settings.copy(glassBorderWidthDp = it)) }

    StudioSlider(
        label = "Top Reflective Highlight Opacity",
        value = settings.glassHighlightOpacity * 100f,
        range = 0f..60f,
        suffix = "%"
    ) { onUpdate(settings.copy(glassHighlightOpacity = it / 100f)) }

    StudioSlider(
        label = "Shadow Elevation",
        value = settings.glassShadowElevationDp,
        range = 0f..20f,
        suffix = "dp"
    ) { onUpdate(settings.copy(glassShadowElevationDp = it)) }
}

@Composable
private fun ToolbarSettingsSection(
    settings: AppearanceSettings,
    onUpdate: (AppearanceSettings) -> Unit
) {
    Text("Expanded Toolbar Layout", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(8.dp))

    StudioSlider(
        label = "Toolbar Height",
        value = settings.toolbarHeightDp,
        range = 44f..64f,
        suffix = "dp"
    ) { onUpdate(settings.copy(toolbarHeightDp = it)) }

    StudioSlider(
        label = "Max Toolbar Width",
        value = settings.toolbarMaxWidthDp,
        range = 240f..400f,
        suffix = "dp"
    ) { onUpdate(settings.copy(toolbarMaxWidthDp = it)) }

    StudioSlider(
        label = "Chip Spacing",
        value = settings.chipSpacingDp,
        range = 2f..14f,
        suffix = "dp"
    ) { onUpdate(settings.copy(chipSpacingDp = it)) }

    StudioSlider(
        label = "Chip Icon Size",
        value = settings.chipIconSizeDp,
        range = 14f..24f,
        suffix = "dp"
    ) { onUpdate(settings.copy(chipIconSizeDp = it)) }

    StudioSlider(
        label = "Chip Label Size",
        value = settings.chipLabelSizeSp,
        range = 10f..16f,
        suffix = "sp"
    ) { onUpdate(settings.copy(chipLabelSizeSp = it)) }
}

@Composable
private fun ActionsSettingsSection(
    settings: AppearanceSettings,
    onUpdate: (AppearanceSettings) -> Unit
) {
    Text("Customize Quick-Action Toolbar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Text(
        text = "Tap any action to toggle between the Quick Toolbar and the secondary More panel.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))

    Text("Active in Quick Toolbar:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(6.dp))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        settings.activeQuickActions.forEach { actionId ->
            val item = DefaultActionItems.findById(actionId) ?: return@forEach
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(item.label, fontSize = 13.sp)
                    }

                    if (settings.activeQuickActions.size > 2) {
                        IconButton(
                            onClick = {
                                val newActive = settings.activeQuickActions.filter { it != actionId }
                                val newMore = settings.moreActions + actionId
                                onUpdate(settings.copy(activeQuickActions = newActive, moreActions = newMore))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move to More", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text("Available under More Options:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(6.dp))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        settings.moreActions.forEach { actionId ->
            val item = DefaultActionItems.findById(actionId) ?: return@forEach
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(item.label, fontSize = 13.sp)
                    }

                    IconButton(
                        onClick = {
                            val newMore = settings.moreActions.filter { it != actionId }
                            val newActive = settings.activeQuickActions + actionId
                            onUpdate(settings.copy(activeQuickActions = newActive, moreActions = newMore))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move to Quick Toolbar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("${String.format("%.1f", value)} $suffix", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
