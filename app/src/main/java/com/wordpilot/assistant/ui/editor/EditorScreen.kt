package com.wordpilot.assistant.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordpilot.assistant.ui.suggestions.SuggestionList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    documentId: Int? = null,
    initialText: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToRewrite: (String) -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        if (documentId != null && documentId > 0) {
            viewModel.loadDocument(documentId)
        }
    }

    LaunchedEffect(initialText) {
        if (!initialText.isNullOrBlank() && uiState.text.isBlank()) {
            viewModel.onTextChanged(initialText)
            viewModel.checkText()
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Text") },
            text = { Text("Clear all text? This action can be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearText()
                    showClearDialog = false
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Writing Editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(uiState.text))
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = {
                        val clip = clipboardManager.getText()
                        if (clip != null) {
                            viewModel.onTextChanged(uiState.text + clip.text)
                        }
                    }) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Stats bar
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${uiState.wordCount} words · ${uiState.charCount} chars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (uiState.corrections.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text("${uiState.corrections.size}")
                                }
                            }

                            TextButton(onClick = {
                                if (showSuggestions) {
                                    showSuggestions = false
                                } else {
                                    viewModel.checkText()
                                    showSuggestions = true
                                }
                            }) {
                                if (uiState.isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(if (showSuggestions) "Hide" else "Check Writing")
                            }

                            if (uiState.text.isNotBlank()) {
                                TextButton(onClick = { onNavigateToRewrite(uiState.text) }) {
                                    Text("Rewrite")
                                }
                            }

                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Text editor area
            val scrollState = rememberScrollState()
            
            Box(
                modifier = Modifier
                    .weight(if (showSuggestions && uiState.corrections.isNotEmpty()) 0.55f else 1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                if (uiState.text.isEmpty()) {
                    Text(
                        "Start typing or paste your text here…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                BasicTextField(
                    value = uiState.text,
                    onValueChange = { viewModel.onTextChanged(it) },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            // Suggestions panel
            if (showSuggestions) {
                HorizontalDivider()
                SuggestionList(
                    corrections = uiState.corrections,
                    onAccept = { viewModel.acceptCorrection(it) },
                    onDismiss = { viewModel.dismissCorrection(it) },
                    onAddToDictionary = { viewModel.addToDictionary(it) },
                    onIgnoreRule = { viewModel.ignoreRule(it) },
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}
