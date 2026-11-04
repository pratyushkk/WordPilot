package com.wordpilot.assistant.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wordpilot.assistant.R
import com.wordpilot.assistant.overlay.WordPilotAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDictionary: () -> Unit,
    onNavigateToApiSettings: () -> Unit,
    onNavigateToBubbleCustomization: () -> Unit = {},
    onNavigateToAiWindowStudio: () -> Unit = {}
) {
    val context = LocalContext.current
    var showAccessibilityGuide by remember { mutableStateOf(false) }
    val isServiceActive = WordPilotAccessibilityService.instance != null
    val app = com.wordpilot.assistant.WordPilotApp.instance
    val geminiApiKey by app.userPreferences.geminiApiKey.collectAsState(initial = null)

    if (showAccessibilityGuide) {
        AccessibilityGuideDialog(
            isServiceActive = isServiceActive,
            onDismiss = { showAccessibilityGuide = false },
            onOpenAccessibilitySettings = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try { context.startActivity(intent) } catch (e: Exception) {}
            },
            onOpenAppSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try { context.startActivity(intent) } catch (e: Exception) {}
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_wordpilot_logo),
                            contentDescription = "WordPilot Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                "WordPilot",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Your AI Writing Companion v2.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Primary Writing Assistant Hero Card
            Card(
                onClick = onNavigateToEditor,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Open Writing Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Type or paste text for instant grammar, spelling & tone correction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null
                    )
                }
            }

            // Quick Service Status Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isServiceActive) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isServiceActive) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAccessibilityGuide = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Text(
                            text = if (isServiceActive) "Floating Assistant is Active & Ready" else "Floating Assistant Inactive — Tap to Setup",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isServiceActive) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        Icons.Filled.HelpOutline,
                        contentDescription = "Setup Guide",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

                        if (geminiApiKey.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gemini Model Setup Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Enter your Gemini API key to activate floating AI Rewriting and Tone Adjustments.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {}
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Get Free Key", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onNavigateToApiSettings,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6)
                                ),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enter Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "Gemini Model Connected & Active",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                        TextButton(
                            onClick = onNavigateToApiSettings,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Manage", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Main action cards
            HomeActionCard(
                icon = Icons.Outlined.CloudQueue,
                title = "API Providers & Keys",
                description = "Configure Sapling (Primary) & LanguageTool automatic failover",
                onClick = onNavigateToApiSettings
            )

            HomeActionCard(
                icon = Icons.Outlined.Assistant,
                title = "Floating Assistant (Cross-App)",
                description = if (isServiceActive) "Active. Tap to view status or open accessibility settings" else "Enable floating bubble to check writing directly inside other apps",
                onClick = { showAccessibilityGuide = true }
            )

            HomeActionCard(
                icon = Icons.Outlined.HelpOutline,
                title = "Accessibility & Auto-Pause Guide",
                description = "Complete steps to enable service, banking shield & how to resume",
                onClick = { showAccessibilityGuide = true }
            )

                        HomeActionCard(
                icon = Icons.Outlined.Palette,
                title = "Appearance & Layout Studio",
                description = "Customize bubble size, AI logo icon, error badge & 30% edge tuck",
                onClick = onNavigateToBubbleCustomization
            )

            HomeActionCard(
                icon = Icons.Outlined.AutoAwesome,
                title = "AI Window Studio",
                description = "Customize floating AI chat window size, glass theme, anchors & custom prompts",
                onClick = onNavigateToAiWindowStudio
            )

            HomeActionCard(
                icon = Icons.Outlined.Book,
                title = "Personal Dictionary",
                description = "Manage custom words and ignored suggestions",
                onClick = onNavigateToDictionary
            )

            HomeActionCard(
                icon = Icons.Outlined.History,
                title = "Document History",
                description = "View and manage saved documents",
                onClick = onNavigateToHistory
            )

            HomeActionCard(
                icon = Icons.Outlined.Tune,
                title = "Settings",
                description = "Themes, auto-check preferences, and banking shield",
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "WordPilot uses lightweight external APIs with automatic failover. 50+ banking and payment apps are automatically shielded for privacy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AccessibilityGuideDialog(
    isServiceActive: Boolean,
    onDismiss: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Assistant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Floating Assistant Guide",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Setup, Auto-Pause & Resume",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isServiceActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isServiceActive) Color(0xFF10B981) else Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                if (isServiceActive) Icons.Filled.CheckCircle else Icons.Filled.Info,
                                contentDescription = null,
                                tint = if (isServiceActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (isServiceActive)
                                    "Status: Active & Running. Floating bubble appears automatically whenever you type."
                                else
                                    "Status: Inactive. Turn on the Accessibility Service to enable cross-app floating assistant.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Section 1: How to Turn On
                    GuideSectionCard(
                        stepNumber = "1",
                        title = "How to Turn On Accessibility Service",
                        badge = "Required"
                    ) {
                        Text(
                            "Follow these 3 quick steps in your phone's settings:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GuideStepItem(number = "1", text = "Tap the 'Open Accessibility Settings' button below.")
                        GuideStepItem(number = "2", text = "Scroll to 'Downloaded apps' (or 'Installed services').")
                        GuideStepItem(number = "3", text = "Select 'WordPilot Assistant', turn the toggle ON, and tap 'Allow'.")

                        Spacer(modifier = Modifier.height(8.dp))

                        // MIUI / ColorOS / HyperOS Tip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Outlined.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "If Android shows 'Restricted setting': Go to Phone Settings ➔ Apps ➔ WordPilot ➔ 3 dots (top right) ➔ tap 'Allow restricted settings', then turn on accessibility.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Accessibility Settings", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Section 2: Auto-Pause & How to Resume
                    GuideSectionCard(
                        stepNumber = "2",
                        title = "How Auto-Pause & Resume Works",
                        badge = "Privacy Shield"
                    ) {
                        Text(
                            "WordPilot is built with bank-grade privacy protection:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        GuideStepItem(
                            number = "🔒",
                            text = "Auto-Pause in 50+ Banking Apps: The bubble completely hides and pauses in apps like Google Pay, PhonePe, Paytm, YONO SBI, HDFC, CRED, and password fields."
                        )
                        GuideStepItem(
                            number = "⚡",
                            text = "Auto-Resume: When you exit the banking app and return to any regular app (WhatsApp, Chrome, Notes, Gmail) or tap an editable text field, WordPilot detects the safe context and resumes automatically!"
                        )
                        GuideStepItem(
                            number = "🎛️",
                            text = "Quick Settings Tile: Swipe down your Android notification panel and tap the 'WordPilot' tile anytime to toggle or resume instantly."
                        )
                        GuideStepItem(
                            number = "📱",
                            text = "Home Screen Widget: Add WordPilot's 1-tap widget to your home screen for quick single-touch resumption."
                        )
                    }

                    // Section 3: Battery Optimization
                    GuideSectionCard(
                        stepNumber = "3",
                        title = "Prevent Android Background Killing",
                        badge = "Recommended"
                    ) {
                        Text(
                            "Some Android phone brands (Samsung, Xiaomi, OnePlus, Motorola) aggressively kill background services to save battery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "To ensure WordPilot is always available when typing, set its Battery Usage to 'Unrestricted' in App Info.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open App Battery Settings")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It, Close Guide", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    stepNumber: String,
    title: String,
    badge: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(stepNumber, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun GuideStepItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            number.let { if (it.length == 1 && it[0].isDigit()) "$it." else it },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
    }
}
