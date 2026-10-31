package com.wordpilot.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wordpilot.assistant.ui.navigation.Routes
import com.wordpilot.assistant.ui.navigation.WordPilotNavGraph
import com.wordpilot.assistant.ui.theme.WordPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isSendAction = intent?.action == Intent.ACTION_SEND && intent?.type?.startsWith("text/") == true
        val sharedText = if (isSendAction) intent?.getStringExtra(Intent.EXTRA_TEXT) else null
        val startDest = if (!sharedText.isNullOrBlank()) {
            Routes.EDITOR
        } else {
            intent?.getStringExtra("navigate_to") ?: Routes.HOME
        }

        setContent {
            val app = application as WordPilotApp
            val theme by app.userPreferences.theme.collectAsState(initial = "system")

            WordPilotTheme(themePreference = theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WordPilotNavGraph(
                        startDestination = startDest,
                        initialSharedText = sharedText
                    )
                }
            }
        }
    }
}
