package com.wordpilot.assistant.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.ModelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ModelUiState(
    val isInstalled: Boolean = false,
    val modelState: ModelState = ModelState.NOT_INSTALLED,
    val modelName: String = "Meta Llama 3.2 (1B Instruct)",
    val modelSource: String = "Hugging Face (bartowski/Llama-3.2-1B-Instruct-GGUF)",
    val modelLicense: String = "Llama 3.2 Community License",
    val storageMb: Long = 0,
    val downloadProgress: Float = 0f,
    val downloadedMb: Long = 0,
    val totalMb: Long = 770,
    val statusText: String = "Not installed",
    val errorMessage: String? = null,
    val memoryMb: Long = 0,
    val loadTimeMs: Long = 0,
    val inferenceMs: Long = 0
)

class ModelManagementViewModel : ViewModel() {
    private val app = WordPilotApp.instance
    private val repo = app.modelRepository
    private val localEngine = app.aiEngine

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.modelState,
                repo.downloadProgress,
                repo.downloadedBytes,
                repo.totalBytes,
                repo.downloadErrorMessage
            ) { state, progress, downloaded, total, error ->
                val info = repo.getModelInfo()
                val resourceUsage = localEngine.getResourceUsage()
                val isInstalled = state == ModelState.INSTALLED || repo.isModelInstalled()

                val statusText = when (state) {
                    ModelState.DOWNLOADING -> "Downloading (${(progress * 100).toInt()}%)"
                    ModelState.INSTALLED, ModelState.READY -> "Model Ready & Active"
                    ModelState.ERROR -> "Download Failed"
                    else -> if (isInstalled) "Model Ready & Active" else "Ready to Download"
                }

                ModelUiState(
                    isInstalled = isInstalled,
                    modelState = state,
                    modelName = info.name,
                    modelSource = info.source,
                    modelLicense = info.license,
                    storageMb = info.installedSizeMb,
                    downloadProgress = progress,
                    downloadedMb = downloaded / (1024 * 1024),
                    totalMb = if (total > 0) total / (1024 * 1024) else 770L,
                    statusText = statusText,
                    errorMessage = error,
                    memoryMb = resourceUsage.memoryUsageMb,
                    loadTimeMs = resourceUsage.loadTimeMs,
                    inferenceMs = resourceUsage.lastInferenceTimeMs
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun startDownload() {
        if (_uiState.value.modelState == ModelState.DOWNLOADING) return
        viewModelScope.launch {
            repo.downloadModel()
        }
    }

    fun cancelDownload() {
        repo.cancelDownload()
    }

    fun deleteModel() {
        repo.deleteModel()
    }
}
