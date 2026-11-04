package com.wordpilot.assistant.domain.models

data class ModelInfo(
    val name: String,
    val source: String,
    val license: String,
    val downloadSizeMb: Long,
    val installedSizeMb: Long,
    val isInstalled: Boolean,
    val filePath: String? = null
)

data class ResourceUsage(
    val memoryUsageMb: Long = 0,
    val isModelLoaded: Boolean = false,
    val loadTimeMs: Long = 0,
    val lastInferenceTimeMs: Long = 0
)

enum class ModelState {
    NOT_INSTALLED,
    DOWNLOADING,
    INSTALLED,
    LOADING,
    READY,
    ERROR
}
