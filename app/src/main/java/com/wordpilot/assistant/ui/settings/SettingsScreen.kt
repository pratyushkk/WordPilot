package com.wordpilot.assistant.ui.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDictionary: () -> Unit,
    onNavigateToModel: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToBubbleCustomization: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

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
                    "• No text is transmitted to external servers\n" +
                    "• No analytics or telemetry data is collected\n" +
                    "• The optional AI model runs entirely on-device\n" +
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
                    title = "Popup Size & Reshape",
                    subtitle = "Sliders for width, height, shape, text size with live preview",
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

            // Keyboard
            SettingsSection(title = "Keyboard") {
                SettingsClickable(
                    title = "Enable WordPilot Keyboard",
                    subtitle = "Set up the keyboard in system settings",
                    onClick = onNavigateToOnboarding
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

            // AI
            SettingsSection(title = "AI Model") {
                SettingsClickable(
                    title = "Manage AI Model",
                    subtitle = "Download, configure, or remove the on-device AI model",
                    onClick = onNavigateToModel
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
