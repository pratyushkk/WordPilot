package com.wordpilot.assistant.ui.settings

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.domain.models.CorrectionSeverity
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bubble, 1: Gradient, 2: Glass, 3: Toolbar

    // Preview state inside studio
    var previewUiState by remember { mutableStateOf(FloatingUiState.EXPANDED) }

    val defaultDemoCorrections = remember {
        listOf(
            Correction(
                id = "demo-1",
                startOffset = 4,
                endOffset = 13,
                originalText = "have went",
                suggestedReplacement = "has gone",
                category = CorrectionCategory.GRAMMAR,
                severity = CorrectionSeverity.ERROR,
                ruleId = "SAPLING_GRAMMAR"
            ),
            Correction(
                id = "demo-2",
                startOffset = 19,
                endOffset = 26,
                originalText = "a apple",
                suggestedReplacement = "an apple",
                category = CorrectionCategory.GRAMMAR,
                severity = CorrectionSeverity.ERROR,
                ruleId = "SAPLING_ARTICLE"
            )
        )
    }
    var previewCorrections by remember { mutableStateOf(defaultDemoCorrections) }

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
                            previewCorrections = defaultDemoCorrections
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "LIVE PREVIEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (previewCorrections.isEmpty()) {
                                TextButton(
                                    onClick = { previewCorrections = defaultDemoCorrections },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text("Reset Errors", fontSize = 10.sp)
                                }
                            }
                        }

                        // Mode toggle chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PreviewModeChip("Collapsed", previewUiState == FloatingUiState.COLLAPSED) {
                                previewUiState = FloatingUiState.COLLAPSED
                            }
                            PreviewModeChip("Expanded", previewUiState == FloatingUiState.EXPANDED) {
                                previewUiState = FloatingUiState.EXPANDED
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated Screen Canvas
                    val tuckOffset = if (previewUiState == FloatingUiState.COLLAPSED && settings.edgeTuckEnabled) {
                        -(settings.bubbleSizeDp * settings.edgeTuckPercent + 12f).dp
                    } else {
                        0.dp
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F1115))
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(modifier = Modifier.offset(x = tuckOffset)) {
                            FloatingAssistantView(
                            settings = settings,
                            uiState = previewUiState,
                            isNearRightEdge = false,
                            errorCount = previewCorrections.size,
                            isChecking = false,
                            activeCorrection = previewCorrections.firstOrNull(),
                            activeCorrections = previewCorrections,
                            previewOriginalText = "She have went to a apple store.",
                            previewImprovedText = "She has gone to an apple store.",
                            isPreviewLoading = false,
                            previewErrorMessage = null,
                            canUndo = true,
                            currentTone = "Default",
                            onBubbleClick = {
                                previewUiState = if (previewUiState == FloatingUiState.COLLAPSED) FloatingUiState.EXPANDED else FloatingUiState.COLLAPSED
                            },
                            onDrag = { _, _ -> },
                            onActionClick = {},
                            onCustomPromptSubmit = {},
                            onSelectTone = {},
                            onReplace = {},
                            onInsert = {},
                            onCopy = {},
                            onCancelPreview = { previewUiState = FloatingUiState.COLLAPSED },
                            onUndo = {},
                            onOpenSettings = {},
                            onCollapse = { previewUiState = FloatingUiState.COLLAPSED },
                            onAcceptCorrection = { accepted ->
                                previewCorrections = previewCorrections.filter { it.id != accepted.id }
                                if (previewCorrections.isEmpty()) {
                                    previewUiState = FloatingUiState.COLLAPSED
                                }
                            },
                            onDismissCorrection = {
                                if (previewCorrections.isNotEmpty()) {
                                    previewCorrections = previewCorrections.drop(1)
                                }
                            }
                        )
                        }
                    }
                }
            }

            // Quick Style Presets Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                listOf(
                    Triple("Compact", 36f, 18f),
                    Triple("Default", 48f, 24f),
                    Triple("Large", 56f, 28f),
                    Triple("Minimal", 32f, 16f)
                ).forEach { (name, bSize, iSize) ->
                    val isMatch = settings.bubbleSizeDp == bSize && settings.iconSizeDp == iSize
                    SuggestionChip(
                        onClick = {
                            val newS = when (name) {
                                "Compact" -> settings.copy(bubbleSizeDp = 36f, iconSizeDp = 18f, edgeTuckPercent = 0.35f, backgroundOpacity = 0.85f)
                                "Default" -> settings.copy(bubbleSizeDp = 48f, iconSizeDp = 24f, edgeTuckPercent = 0.30f, backgroundOpacity = 0.90f)
                                "Large" -> settings.copy(bubbleSizeDp = 56f, iconSizeDp = 28f, edgeTuckPercent = 0.25f, backgroundOpacity = 0.95f)
                                "Minimal" -> settings.copy(bubbleSizeDp = 32f, iconSizeDp = 16f, edgeTuckPercent = 0.40f, backgroundOpacity = 0.70f)
                                else -> settings
                            }
                            updateAndSave(newS)
                        },
                        label = { Text(name, fontSize = 11.sp, fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isMatch) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            labelColor = if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.height(28.dp)
                    )
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
                    text = { Text("Bubble & Logo", fontSize = 12.sp) }
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
    Text("Bubble Sizing (Smaller / Bigger)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Text(
        "Make the minimized circle bubble smaller or bigger for optimal screen reach and tap comfort.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(10.dp))

    // 5 Quick Sizing Presets
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            "XS (28dp)" to 28f,
            "Small (38dp)" to 38f,
            "Standard (48dp)" to 48f,
            "Large (60dp)" to 60f,
            "XL (72dp)" to 72f
        ).forEach { (label, size) ->
            val isSelected = kotlin.math.abs(settings.bubbleSizeDp - size) < 2f
            OutlinedButton(
                onClick = { onUpdate(settings.copy(bubbleSizeDp = size)) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label.split(" ")[0],
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    StudioSlider(
        label = "Custom Diameter: ${settings.bubbleSizeDp.toInt()} dp",
        value = settings.bubbleSizeDp,
        range = 24f..80f,
        suffix = "dp"
    ) { onUpdate(settings.copy(bubbleSizeDp = it)) }

    Text(
        text = when {
            settings.bubbleSizeDp < 34f -> "Extra small — minimal screen obstruction, discreet"
            settings.bubbleSizeDp < 44f -> "Compact — clean, space-saving"
            settings.bubbleSizeDp < 54f -> "Standard — optimal balance of visibility and tap comfort"
            settings.bubbleSizeDp < 66f -> "Large — high visibility and prominent number display"
            else -> "Extra large — maximum readability with huge tap target"
        },
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(16.dp))

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

    if (settings.bubbleShape == BubbleShape.CUSTOM) {
        Spacer(modifier = Modifier.height(12.dp))
        StudioSlider(
            label = "Custom Corner Radius",
            value = settings.bubbleCornerRadiusDp,
            range = 0f..40f,
            suffix = "dp"
        ) { onUpdate(settings.copy(bubbleCornerRadiusDp = it)) }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // =========================================================================
    // AI LOGO IN EDITING SECTION (Multiple Options & Controls)
    // =========================================================================
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = settings.getAiLogo().icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column {
                    Text("AI Logo in Editing Section", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Select the AI star icon shown in the floating bubble and editing toolbar",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Choose Icon Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))

            // 12 Rich AI Logo Options in a 2-column grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AiLogoIcon.values().toList().chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { logo ->
                            val isSelected = settings.aiLogoIconId == logo.id
                            Surface(
                                onClick = { onUpdate(settings.copy(aiLogoIconId = logo.id)) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    if (isSelected) 1.8.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = logo.icon,
                                                contentDescription = logo.displayName,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = logo.displayName,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = logo.description,
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            StudioSlider(
                label = "Logo Icon Size: ${settings.iconSizeDp.toInt()} dp",
                value = settings.iconSizeDp,
                range = 14f..40f,
                suffix = "dp"
            ) { onUpdate(settings.copy(iconSizeDp = it)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Small (18dp)" to 18f,
                    "Standard (24dp)" to 24f,
                    "Large (30dp)" to 30f,
                    "XL (36dp)" to 36f
                ).forEach { (label, dpVal) ->
                    val isSel = kotlin.math.abs(settings.iconSizeDp - dpVal) < 1.5f
                    OutlinedButton(
                        onClick = { onUpdate(settings.copy(iconSizeDp = dpVal)) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        Text(label.split(" ")[0], fontSize = 10.5.sp)
                    }
                }
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

    // 1. Edge Docking & 30% Tuck
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Dock & Tuck into Edge", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Snap to extreme screen edge and hide 30% inside when minimized to minimize obstruction.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = settings.edgeTuckEnabled,
                    onCheckedChange = { onUpdate(settings.copy(edgeTuckEnabled = it)) }
                )
            }

            if (settings.edgeTuckEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                StudioSlider(
                    label = "Tuck Depth: ${(settings.edgeTuckPercent * 100).toInt()}% hidden",
                    value = settings.edgeTuckPercent * 100f,
                    range = 15f..50f,
                    suffix = "%"
                ) { onUpdate(settings.copy(edgeTuckPercent = it / 100f)) }
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

    // 2. Error Badge Number & Letter Customization
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Error Badge Number & Letter", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "Customize the error counter format, letter tag, and text size on the minimized bubble.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text("Badge Format / Position", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BadgeLetterPosition.values().forEach { pos ->
                    val isSelected = settings.badgeLetterPosition == pos
                    OutlinedButton(
                        onClick = { onUpdate(settings.copy(badgeLetterPosition = pos)) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (pos) {
                                BadgeLetterPosition.NONE -> "Number Only"
                                BadgeLetterPosition.PREFIX -> "Prefix (E2)"
                                BadgeLetterPosition.SUFFIX -> "Suffix (2e)"
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            if (settings.badgeLetterPosition != BadgeLetterPosition.NONE) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.badgeLetter,
                    onValueChange = { onUpdate(settings.copy(badgeLetter = it.take(4))) },
                    label = { Text("Badge Letter / Tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Suggestions:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("E", "e", "!", "#", "err").forEach { tag ->
                        SuggestionChip(
                            onClick = { onUpdate(settings.copy(badgeLetter = tag)) },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            StudioSlider(
                label = "Badge Text Size: ${settings.badgeTextSizeSp.toInt()} sp",
                value = settings.badgeTextSizeSp,
                range = 10f..26f,
                suffix = "sp"
            ) { onUpdate(settings.copy(badgeTextSizeSp = it)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Small (14sp)" to 14f,
                    "Medium (18sp)" to 18f,
                    "Large (22sp)" to 22f,
                    "XL (26sp)" to 26f
                ).forEach { (label, spVal) ->
                    val isSel = kotlin.math.abs(settings.badgeTextSizeSp - spVal) < 1.5f
                    OutlinedButton(
                        onClick = { onUpdate(settings.copy(badgeTextSizeSp = spVal)) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        Text(label.split(" ")[0], fontSize = 10.5.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Extra Bold / Black Font Weight", fontSize = 12.sp)
                Switch(
                    checked = settings.badgeFontWeightBold,
                    onCheckedChange = { onUpdate(settings.copy(badgeFontWeightBold = it)) }
                )
            }
        }
    }

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
        label = "Shadow Elevation",
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
    Text("Correction Pill & Toolbar Layout", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Text(
        "Fine-tune the size, width and typography of the expanded 1-tap correction pill.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(10.dp))

    StudioSlider(
        label = "Toolbar Height",
        value = settings.toolbarHeightDp,
        range = 40f..64f,
        suffix = "dp"
    ) { onUpdate(settings.copy(toolbarHeightDp = it)) }

    StudioSlider(
        label = "Max Toolbar Width",
        value = settings.toolbarMaxWidthDp,
        range = 220f..420f,
        suffix = "dp"
    ) { onUpdate(settings.copy(toolbarMaxWidthDp = it)) }

    StudioSlider(
        label = "Chip Spacing",
        value = settings.chipSpacingDp,
        range = 2f..14f,
        suffix = "dp"
    ) { onUpdate(settings.copy(chipSpacingDp = it)) }

    StudioSlider(
        label = "Chip Label Size",
        value = settings.chipLabelSizeSp,
        range = 11f..16f,
        suffix = "sp"
    ) { onUpdate(settings.copy(chipLabelSizeSp = it)) }
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
