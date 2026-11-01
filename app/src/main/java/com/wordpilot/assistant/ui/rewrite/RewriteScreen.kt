package com.wordpilot.assistant.ui.rewrite

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.RewriteStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RewriteScreen(
    onNavigateBack: () -> Unit,
    viewModel: RewriteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewrite") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Input text
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInputText(it) },
                label = { Text("Text to rewrite") },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp),
                maxLines = 10
            )

            // AI Engine Selector
            Text(
                "AI Engine",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedEngine == AIEngineType.RULE_BASED,
                    onClick = { viewModel.setSelectedEngine(AIEngineType.RULE_BASED) },
                    label = { Text("⚡ Offline Rules") },
                    leadingIcon = if (uiState.selectedEngine == AIEngineType.RULE_BASED) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )

                FilterChip(
                    selected = uiState.selectedEngine == AIEngineType.LOCAL_AI,
                    onClick = { viewModel.setSelectedEngine(AIEngineType.LOCAL_AI) },
                    label = { Text("📱 Local Llama 3.2") },
                    leadingIcon = if (uiState.selectedEngine == AIEngineType.LOCAL_AI) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )

                FilterChip(
                    selected = uiState.selectedEngine == AIEngineType.GEMINI_FLASH,
                    onClick = { viewModel.setSelectedEngine(AIEngineType.GEMINI_FLASH) },
                    label = { Text("☁️ Cloud API") },
                    leadingIcon = if (uiState.selectedEngine == AIEngineType.GEMINI_FLASH) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }

            // Style selector
            Text(
                "Rewrite Style",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RewriteStyle.entries.forEach { style ->
                    if (style != RewriteStyle.CUSTOM) {
                        FilterChip(
                            selected = uiState.selectedStyle == style,
                            onClick = { viewModel.selectStyle(style) },
                            label = { Text(style.displayName, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // Custom instruction
            if (uiState.selectedStyle == RewriteStyle.CUSTOM) {
                OutlinedTextField(
                    value = uiState.customInstruction,
                    onValueChange = { viewModel.updateCustomInstruction(it) },
                    label = { Text("Custom instruction") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            // Rewrite button
            Button(
                onClick = { viewModel.rewrite() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.inputText.isNotBlank() && !uiState.isProcessing
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Rewrite")
            }

            // Result
            if (uiState.result != null) {
                val result = uiState.result!!
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (result.isSuccess) {
                            Text(
                                "Original",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                result.originalText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Text(
                                "Rewritten",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                result.rewrittenText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    viewModel.updateInputText(result.rewrittenText)
                                }) {
                                    Text("Apply")
                                }
                                OutlinedButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(result.rewrittenText))
                                }) {
                                    Text("Copy")
                                }
                                TextButton(onClick = { viewModel.rewrite() }) {
                                    Text("Try Again")
                                }
                            }
                        } else {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                result.errorMessage ?: "Rewrite failed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
