package com.wordpilot.assistant.keyboard.expanded

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordpilot.assistant.R
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.keyboard.AssistantSessionState
import com.wordpilot.assistant.keyboard.AssistantSourceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedWritingAssistant(
    session: AssistantSessionState,
    onClose: () -> Unit,
    onApply: (String) -> Unit,
    onRefreshText: () -> Unit,
    onUpdateSourceText: (String) -> Unit,
    onSelectStyle: (RewriteStyle) -> Unit,
    onCustomInstructionChanged: (String) -> Unit,
    onSelectEngine: (AIEngineType) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var editableResultText by remember(session.rewrittenText) {
        mutableStateOf(session.rewrittenText)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Preview, 1: Diff
    var showModelMenu by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var copyConfirmation by remember { mutableStateOf(false) }

    val panelBackground = if (isDark) Color(0xFF14171A) else Color(0xFFFBFBFD)
    val cardBackground = if (isDark) Color(0xFF1F2328) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF333842) else Color(0xFFE2E4E8)
    val primaryBlue = Color(0xFF1976D2)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 265.dp, max = 295.dp),
        color = panelBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // -------------------------------------------------------------
            // 1. HEADER ROW: Logo, Brand, Engine Selector, Close Button
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_wordpilot_logo),
                        contentDescription = "WordPilot",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Column {
                        Text(
                            text = "WordPilot",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "Writing assistant",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Engine Selector Pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF282D35) else Color(0xFFEEF2F6),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3E4550) else Color(0xFFD4DAE2)),
                            modifier = Modifier
                                .clickable { showModelMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val engineIcon = when (session.selectedEngine) {
                                    AIEngineType.GEMINI_FLASH -> "⚡"
                                    AIEngineType.LOCAL_AI -> "📱"
                                    AIEngineType.RULE_BASED -> "🛡"
                                }
                                Text(
                                    text = "$engineIcon ${session.selectedEngine.shortName}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select engine",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Engine Dropdown Menu
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            for (engine in AIEngineType.entries) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = engine.displayName,
                                                    fontWeight = if (session.selectedEngine == engine) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (engine == AIEngineType.GEMINI_FLASH) primaryBlue.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = engine.badge,
                                                        fontSize = 9.5.sp,
                                                        color = if (engine == AIEngineType.GEMINI_FLASH) primaryBlue
                                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 3.dp)
                                                    )
                                                }
                                            }
                                            if (engine == AIEngineType.GEMINI_FLASH && WordPilotApp.instance.cachedGeminiApiKey.isNullOrBlank()) {
                                                Text(
                                                    text = "Tap to configure API key",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        showModelMenu = false
                                        onSelectEngine(engine)
                                        if (engine == AIEngineType.GEMINI_FLASH && WordPilotApp.instance.cachedGeminiApiKey.isNullOrBlank()) {
                                            showApiKeyDialog = true
                                        }
                                    },
                                    leadingIcon = {
                                        if (session.selectedEngine == engine) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = primaryBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close assistant",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // -------------------------------------------------------------
            // 2. SCROLLABLE WORKSPACE CONTENT
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // A. "YOUR TEXT" CARD
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBackground,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Your text",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF2C323B) else Color(0xFFEBF1F8)
                                ) {
                                    Text(
                                        text = session.sourceType.displayName,
                                        fontSize = 9.sp,
                                        color = primaryBlue,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val wordCount = session.currentEditText.trim()
                                    .split(Regex("\\s+"))
                                    .count { it.isNotBlank() }
                                Text(
                                    text = "$wordCount ${if (wordCount == 1) "word" else "words"}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Re-read text",
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable { onRefreshText() },
                                    tint = primaryBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Source Text Display / Field
                        OutlinedTextField(
                            value = session.currentEditText,
                            onValueChange = { onUpdateSourceText(it) },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            placeholder = {
                                Text(
                                    text = "Type or paste a sentence here...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp, max = 68.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // B. OPERATIONS CHIPS (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val operations = listOf(
                        RewriteStyle.FIX_GRAMMAR to "Fix grammar",
                        RewriteStyle.REWRITE to "Rewrite",
                        RewriteStyle.IMPROVE_CLARITY to "Clarity",
                        RewriteStyle.MAKE_PROFESSIONAL to "Professional",
                        RewriteStyle.MAKE_CONCISE to "Concise",
                        RewriteStyle.MAKE_POLITE to "Polite",
                        RewriteStyle.MAKE_CASUAL to "Casual",
                        RewriteStyle.CUSTOM to "Custom..."
                    )

                    for ((style, label) in operations) {
                        val isSelected = session.selectedStyle == style
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectStyle(style) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = if (isDark) Color(0xFF242830) else Color(0xFFECEFF3),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) primaryBlue else cardBorder,
                                selectedBorderColor = primaryBlue,
                                borderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // If Custom is selected, show custom prompt input
                if (session.selectedStyle == RewriteStyle.CUSTOM) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = session.customInstruction,
                            onValueChange = onCustomInstructionChanged,
                            placeholder = { Text("e.g. Make it more persuasive, sound like Steve Jobs...", fontSize = 11.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = onRetry,
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Go", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // C. RESULT PREVIEW & COMPARISON SECTION
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBackground,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Suggested result",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (session.rewrittenText.isNotBlank() && !session.isProcessing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // View Mode Tabs: Preview vs Diff
                                    TabRow(
                                        selectedTabIndex = selectedTab,
                                        modifier = Modifier
                                            .width(130.dp)
                                            .height(26.dp),
                                        indicator = {},
                                        divider = {}
                                    ) {
                                        Tab(
                                            selected = selectedTab == 0,
                                            onClick = { selectedTab = 0 },
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (selectedTab == 0) primaryBlue.copy(alpha = 0.16f)
                                                    else Color.Transparent
                                                )
                                        ) {
                                            Text(
                                                text = "Preview",
                                                fontSize = 10.5.sp,
                                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedTab == 0) primaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Tab(
                                            selected = selectedTab == 1,
                                            onClick = { selectedTab = 1 },
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (selectedTab == 1) primaryBlue.copy(alpha = 0.16f)
                                                    else Color.Transparent
                                                )
                                        ) {
                                            Text(
                                                text = "Diff",
                                                fontSize = 10.5.sp,
                                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedTab == 1) primaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // State Display: Loading, Error, or Result
                        if (session.isProcessing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = primaryBlue
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Polishing with ${session.selectedEngine.shortName}...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (session.errorMessage != null && session.rewrittenText.isBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = session.errorMessage,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (session.selectedEngine == AIEngineType.GEMINI_FLASH) {
                                        Button(
                                            onClick = { showApiKeyDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Set Gemini Key", fontSize = 11.sp)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onSelectEngine(AIEngineType.RULE_BASED) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Use Offline Engine", fontSize = 11.sp)
                                    }
                                }
                            }
                        } else if (session.rewrittenText.isNotBlank()) {
                            if (selectedTab == 0) {
                                // Editable Preview Field
                                OutlinedTextField(
                                    value = editableResultText,
                                    onValueChange = { editableResultText = it },
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 40.dp, max = 80.dp)
                                )
                            } else {
                                // Diff View
                                SuggestionDiffView(
                                    originalText = session.currentEditText.ifBlank { session.originalText },
                                    rewrittenText = editableResultText,
                                    isDark = isDark,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Text(
                                text = "Select an operation above to improve your text.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // -------------------------------------------------------------
            // 3. BOTTOM ACTIONS: Cancel, Try Again, Copy, Apply
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cancel
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Try Again
                    IconButton(
                        onClick = onRetry,
                        enabled = !session.isProcessing,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Try again",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Copy to clipboard
                    OutlinedButton(
                        onClick = {
                            val targetText = editableResultText.ifBlank { session.rewrittenText }
                            if (targetText.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("WordPilot rewrite", targetText)
                                clipboard.setPrimaryClip(clip)
                                copyConfirmation = true
                                coroutineScope.launch {
                                    delay(2000)
                                    copyConfirmation = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = if (copyConfirmation) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (copyConfirmation) "Copied!" else "Copy",
                            fontSize = 12.sp
                        )
                    }

                    // Apply Button (Primary)
                    Button(
                        onClick = {
                            val textToApply = editableResultText.ifBlank { session.rewrittenText }
                            if (textToApply.isNotBlank()) {
                                onApply(textToApply)
                            }
                        },
                        enabled = !session.isProcessing && (editableResultText.isNotBlank() || session.rewrittenText.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Apply",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Apply",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // API KEY DIALOG
    // -------------------------------------------------------------
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    text = "Configure Gemini API",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Gemini API key to enable instant cloud AI grammar fixes, tone rewrites, and explanations.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val key = apiKeyInput.trim()
                        if (key.isNotBlank()) {
                            WordPilotApp.instance.updateGeminiApiKey(key)
                            showApiKeyDialog = false
                            onRetry()
                        }
                    }
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
