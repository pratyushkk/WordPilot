package com.wordpilot.assistant.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.models.AIEngineType
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
    val inferenceMs: Long = 0,
    val selectedEngine: AIEngineType = AIEngineType.GECTOR
)

class ModelManagementViewModel : ViewModel() {
    private val app = WordPilotApp.instance
    private val repo = app.modelRepository
    private val localEngine = app.aiEngine
    private val rewriteCoordinator = app.rewriteCoordinator

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(
                    repo.modelState,
                    repo.downloadProgress,
                    repo.downloadedBytes,
                    repo.totalBytes,
                    repo.downloadErrorMessage
                ) { state, progress, downloaded, total, error ->
                    ModelDownloadData(state, progress, downloaded, total, error)
                },
                rewriteCoordinator.selectedEngine
            ) { downloadData, currentEngine ->
                val info = repo.getModelInfo()
                val resourceUsage = localEngine.getResourceUsage()
                val isInstalled = downloadData.state == ModelState.INSTALLED || repo.isModelInstalled()

                val statusText = when (downloadData.state) {
                    ModelState.DOWNLOADING -> "Downloading (${(downloadData.progress * 100).toInt()}%)"
                    ModelState.INSTALLED, ModelState.READY -> "Model Ready & Active"
                    ModelState.ERROR -> "Download Failed"
                    else -> if (isInstalled) "Model Ready & Active" else "Ready to Download"
                }

                ModelUiState(
                    isInstalled = isInstalled,
                    modelState = downloadData.state,
                    modelName = info.name,
                    modelSource = info.source,
                    modelLicense = info.license,
                    storageMb = info.installedSizeMb,
                    downloadProgress = downloadData.progress,
                    downloadedMb = downloadData.downloaded / (1024 * 1024),
                    totalMb = if (downloadData.total > 0) downloadData.total / (1024 * 1024) else 770L,
                    statusText = statusText,
                    errorMessage = downloadData.error,
                    memoryMb = resourceUsage.memoryUsageMb,
                    loadTimeMs = resourceUsage.loadTimeMs,
                    inferenceMs = resourceUsage.lastInferenceTimeMs,
                    selectedEngine = currentEngine
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    private data class ModelDownloadData(
        val state: ModelState,
        val progress: Float,
        val downloaded: Long,
        val total: Long,
        val error: String?
    )

    fun setSelectedEngine(engine: AIEngineType) {
        viewModelScope.launch {
            app.userPreferences.setSelectedEngine(engine.id)
            rewriteCoordinator.setEngineType(engine)
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
