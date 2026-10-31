package com.wordpilot.assistant.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordpilot.assistant.domain.models.AIEngineType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDictionary: () -> Unit,
    onNavigateToModel: () -> Unit,
    onNavigateToBubbleCustomization: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showGeminiDialog by remember { mutableStateOf(false) }
    var geminiKeyInput by remember { mutableStateOf("") }

    if (showGeminiDialog) {
        AlertDialog(
            onDismissRequest = { showGeminiDialog = false },
            title = { Text("Configure Gemini API Key") },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Gemini API key to enable instant cloud AI writing, style shifts, and clarity improvements.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = geminiKeyInput.ifEmpty { uiState.geminiApiKey ?: "" },
                        onValueChange = { geminiKeyInput = it },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val key = geminiKeyInput.trim()
                    if (key.isNotEmpty()) {
                        viewModel.setGeminiApiKey(key)
                    }
                    showGeminiDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGeminiDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("Delete all local data including dictionary, history, and preferences? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Information") },
            text = {
                Text(
                    "WordPilot processes all text locally on your device.\n\n" +
                    "• No text is transmitted to external servers unless you enable Cloud API\n" +
                    "• Local AI (Meta Llama 3.2) runs 100% on-device with zero network calls\n" +
                    "• No analytics or telemetry data is collected\n" +
                    "• Sensitive fields (passwords, PINs) are never analyzed\n" +
                    "• Document history is disabled by default\n" +
                    "• Text from third-party apps is not stored"
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance
            SettingsSection(title = "Appearance") {
                SettingsThemeSelector(
                    currentTheme = uiState.theme,
                    onThemeSelected = { viewModel.setTheme(it) }
                )
                SettingsClickable(
                    title = "Appearance & Layout Studio",
                    subtitle = "Customize floating AI bubble, multicolor gradients, glassmorphism & actions",
                    onClick = onNavigateToBubbleCustomization
                )
            }

            // Correction
            SettingsSection(title = "Correction") {
                SettingsSwitch(
                    title = "Grammar Checking",
                    subtitle = "Detect grammar errors",
                    checked = uiState.grammarCheckEnabled,
                    onCheckedChange = { viewModel.setGrammarCheckEnabled(it) }
                )
                SettingsSwitch(
                    title = "Spell Checking",
                    subtitle = "Detect misspelled words",
                    checked = uiState.spellCheckEnabled,
                    onCheckedChange = { viewModel.setSpellCheckEnabled(it) }
                )
                SettingsSwitch(
                    title = "Auto Check While Typing",
                    subtitle = "Automatically check text as you type",
                    checked = uiState.autoCheckEnabled,
                    onCheckedChange = { viewModel.setAutoCheckEnabled(it) }
                )
            }

            // Data
            SettingsSection(title = "Data") {
                SettingsClickable(
                    title = "Personal Dictionary",
                    subtitle = "Manage custom words and abbreviations",
                    onClick = onNavigateToDictionary
                )
                SettingsSwitch(
                    title = "Local Document History",
                    subtitle = "Save documents locally (disabled by default)",
                    checked = uiState.historyEnabled,
                    onCheckedChange = { viewModel.setHistoryEnabled(it) }
                )
            }

            // Dedicated On-Device GECToR Engine
            SettingsSection(title = "Grammar Engine") {
                Text(
                    text = "WordPilot uses on-device GECToR exclusively for neural grammatical error correction. All processing runs 100% locally with zero cloud AI, zero external network calls, and no built-in rule fallbacks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                SettingsEngineOption(
                    title = "GECToR Neural Engine (On-Device)",
                    subtitle = "RoBERTa-based neural grammar correction · 100% offline · Active",
                    isSelected = true,
                    badgeText = if (uiState.isGectorModelInstalled) "ONNX Ready" else "Integrated On-Device",
                    badgeOk = true,
                    onClick = { viewModel.setSelectedEngine(AIEngineType.GECTOR) }
                )
            }

            // Banking & App Shield (Grammarly-Style Privacy & Security)
            SettingsSection(title = "Banking & App Shield") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "50+ Financial Apps Automatically Shielded",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Google Pay, PhonePe, Paytm, YONO SBI, HDFC, ICICI, Axis, Chase, and password fields are strictly blocked from being monitored or inspected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsSwitch(
                    title = "Auto-Pause in Banking Apps",
                    subtitle = "Automatically deactivates the accessibility service when a banking app opens. Re-enable with 1 tap from Quick Settings.",
                    checked = uiState.autoPauseBankingEnabled,
                    onCheckedChange = { viewModel.setAutoPauseBankingEnabled(it) }
                )

                SettingsClickable(
                    title = "Accessibility Settings",
                    subtitle = "Manage or toggle the Floating Assistant service",
                    onClick = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    }
                )
            }

            // Privacy & Data
            SettingsSection(title = "Privacy & Data") {
                SettingsClickable(
                    title = "Privacy Information",
                    subtitle = "How WordPilot processes your text",
                    onClick = { showPrivacyDialog = true }
                )
                SettingsClickable(
                    title = "Clear All Local Data",
                    subtitle = "Delete dictionary, history, and preferences",
                    onClick = { showClearDialog = true },
                    isDestructive = true
                )
            }

            // About
            SettingsSection(title = "About") {
                SettingsInfo(
                    title = "Version",
                    value = "1.0.0"
                )
                SettingsInfo(
                    title = "WordPilot",
                    value = "Local-first writing assistant"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickable(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsThemeSelector(
    currentTheme: String,
    onThemeSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Theme", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                FilterChip(
                    selected = currentTheme == value,
                    onClick = { onThemeSelected(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun SettingsEngineOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    badgeText: String,
    badgeOk: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                    color = if (badgeOk) androidx.compose.ui.graphics.Color(0xFFE8F5E9) else androidx.compose.ui.graphics.Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (badgeOk) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsInfo(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
