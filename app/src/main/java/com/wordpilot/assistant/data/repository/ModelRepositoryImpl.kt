package com.wordpilot.assistant.data.repository

import com.wordpilot.assistant.domain.models.ModelInfo
import com.wordpilot.assistant.domain.models.ModelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class ModelRepositoryImpl(private val modelDir: File) {

    private val _modelState = MutableStateFlow(ModelState.NOT_INSTALLED)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    init {
        if (!modelDir.exists()) modelDir.mkdirs()
        checkInstalledModel()
    }

    fun getModelInfo(): ModelInfo {
        val modelFile = getModelFile()
        return ModelInfo(
            name = "WordPilot AI (Stub)",
            source = "Not configured",
            license = "N/A",
            downloadSizeMb = 0,
            installedSizeMb = if (modelFile.exists()) modelFile.length() / (1024 * 1024) else 0,
            isInstalled = modelFile.exists(),
            filePath = if (modelFile.exists()) modelFile.absolutePath else null
        )
    }

    fun getModelFile(): File = File(modelDir, "model.bin")

    fun isModelInstalled(): Boolean = getModelFile().exists()

    fun deleteModel(): Boolean {
        val file = getModelFile()
        val result = if (file.exists()) file.delete() else true
        if (result) _modelState.value = ModelState.NOT_INSTALLED
        return result
    }

    fun setModelState(state: ModelState) {
        _modelState.value = state
    }

    fun setDownloadProgress(progress: Float) {
        _downloadProgress.value = progress
    }

    private fun checkInstalledModel() {
        _modelState.value = if (isModelInstalled()) ModelState.INSTALLED else ModelState.NOT_INSTALLED
    }

    fun getStorageUsedMb(): Long {
        return modelDir.listFiles()?.sumOf { it.length() }?.div(1024 * 1024) ?: 0
    }
}
