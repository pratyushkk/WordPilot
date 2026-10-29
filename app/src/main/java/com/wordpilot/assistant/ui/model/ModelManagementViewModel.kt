package com.wordpilot.assistant.ui.model

import androidx.lifecycle.ViewModel
import com.wordpilot.assistant.WordPilotApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ModelUiState(
    val isInstalled: Boolean = false,
    val modelName: String = "WordPilot AI (Stub)",
    val modelSource: String = "Not configured",
    val modelLicense: String = "N/A",
    val storageMb: Long = 0,
    val statusText: String = "Not installed",
    val memoryMb: Long = 0,
    val loadTimeMs: Long = 0,
    val inferenceMs: Long = 0
)

class ModelManagementViewModel : ViewModel() {
    private val app = WordPilotApp.instance

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    init {
        val modelInfo = app.modelRepository.getModelInfo()
        val resourceUsage = app.aiEngine.getResourceUsage()
        _uiState.value = ModelUiState(
            isInstalled = modelInfo.isInstalled,
            modelName = modelInfo.name,
            modelSource = modelInfo.source,
            modelLicense = modelInfo.license,
            storageMb = modelInfo.installedSizeMb,
            statusText = if (modelInfo.isInstalled) "Installed" else "Not installed",
            memoryMb = resourceUsage.memoryUsageMb,
            loadTimeMs = resourceUsage.loadTimeMs,
            inferenceMs = resourceUsage.lastInferenceTimeMs
        )
    }
}
