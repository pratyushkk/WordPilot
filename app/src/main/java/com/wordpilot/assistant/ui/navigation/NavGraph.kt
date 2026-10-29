package com.wordpilot.assistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wordpilot.assistant.ui.dictionary.DictionaryScreen
import com.wordpilot.assistant.ui.editor.EditorScreen
import com.wordpilot.assistant.ui.history.HistoryScreen
import com.wordpilot.assistant.ui.home.HomeScreen
import com.wordpilot.assistant.ui.model.ModelManagementScreen
import com.wordpilot.assistant.ui.onboarding.OnboardingScreen
import com.wordpilot.assistant.ui.rewrite.RewriteScreen
import com.wordpilot.assistant.ui.settings.BubbleCustomizationScreen
import com.wordpilot.assistant.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val EDITOR_WITH_DOC = "editor/{docId}"
    const val SETTINGS = "settings"
    const val DICTIONARY = "dictionary"
    const val HISTORY = "history"
    const val ONBOARDING = "onboarding"
    const val REWRITE = "rewrite"
    const val MODEL_MANAGEMENT = "model_management"
    const val BUBBLE_CUSTOMIZATION = "bubble_customization"
}

@Composable
fun WordPilotNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToEditor = { navController.navigate(Routes.EDITOR) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToOnboarding = { navController.navigate(Routes.ONBOARDING) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToDictionary = { navController.navigate(Routes.DICTIONARY) },
                onNavigateToModel = { navController.navigate(Routes.MODEL_MANAGEMENT) },
                onNavigateToBubbleCustomization = { navController.navigate(Routes.BUBBLE_CUSTOMIZATION) }
            )
        }
        composable(Routes.EDITOR) {
            EditorScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRewrite = { text -> 
                    navController.navigate(Routes.REWRITE)
                }
            )
        }
        composable(
            Routes.EDITOR_WITH_DOC,
            arguments = listOf(navArgument("docId") { type = NavType.IntType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getInt("docId") ?: -1
            EditorScreen(
                documentId = docId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRewrite = { text ->
                    navController.navigate(Routes.REWRITE)
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDictionary = { navController.navigate(Routes.DICTIONARY) },
                onNavigateToModel = { navController.navigate(Routes.MODEL_MANAGEMENT) },
                onNavigateToOnboarding = { navController.navigate(Routes.ONBOARDING) },
                onNavigateToBubbleCustomization = { navController.navigate(Routes.BUBBLE_CUSTOMIZATION) }
            )
        }
        composable(Routes.BUBBLE_CUSTOMIZATION) {
            BubbleCustomizationScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.DICTIONARY) {
            DictionaryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDocument = { docId -> navController.navigate("editor/$docId") }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.REWRITE) {
            RewriteScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.MODEL_MANAGEMENT) {
            ModelManagementScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
