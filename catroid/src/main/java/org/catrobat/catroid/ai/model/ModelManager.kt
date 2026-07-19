package org.catrobat.catroid.ai.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.catrobat.catroid.ai.settings.AiPreferences
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelManager {

    private const val MODELS_DIR = "ai_models"
    private lateinit var modelsDir: File

    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels.asStateFlow()

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Pair<String, Int>?>(null)
    val downloadProgress: StateFlow<Pair<String, Int>?> = _downloadProgress.asStateFlow()

    private val _downloadSpeed = MutableStateFlow<String?>(null)
    val downloadSpeed: StateFlow<String?> = _downloadSpeed.asStateFlow()

    data class DownloadState(
        val modelId: String = "",
        val progress: Int = 0,
        val speedBytesPerSec: Long = 0,
        val downloadedBytes: Long = 0,
        val totalBytes: Long = 0
    )

    private val _detailedProgress = MutableStateFlow<DownloadState?>(null)
    val detailedProgress: StateFlow<DownloadState?> = _detailedProgress.asStateFlow()

    private val defaultModels = listOf(
        ModelInfo(
            id = "qwen2.5-0.5b-q4",
            name = "Qwen 2.5 0.5B Q4",
            provider = "Qwen",
            size = ModelSize.SIZE_0_5B,
            uri = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            filename = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            description = "Lightweight model for basic tasks"
        ),
        ModelInfo(
            id = "qwen2.5-1b-q4",
            name = "Qwen 2.5 1B Q4",
            provider = "Qwen",
            size = ModelSize.SIZE_1B,
            uri = "https://huggingface.co/Qwen/Qwen2.5-1B-Instruct-GGUF/resolve/main/qwen2.5-1b-instruct-q4_k_m.gguf",
            filename = "qwen2.5-1b-instruct-q4_k_m.gguf",
            description = "Balanced model for most tasks"
        ),
        ModelInfo(
            id = "qwen2.5-3b-q4",
            name = "Qwen 2.5 3B Q4",
            provider = "Qwen",
            size = ModelSize.SIZE_3B,
            uri = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
            filename = "qwen2.5-3b-instruct-q4_k_m.gguf",
            description = "Powerful model for complex analysis"
        ),
        ModelInfo(
            id = "qwen2.5-7b-q4",
            name = "Qwen 2.5 7B Q4",
            provider = "Qwen",
            size = ModelSize.SIZE_7B,
            uri = "https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF/resolve/main/qwen2.5-7b-instruct-q4_k_m.gguf",
            filename = "qwen2.5-7b-instruct-q4_k_m.gguf",
            description = "Maximum quality model"
        )
    )

    fun init(appContext: Context) {
        modelsDir = File(appContext.filesDir, MODELS_DIR).also { it.mkdirs() }
        refreshModels()
    }

    fun refreshModels() {
        val models = defaultModels.map { model ->
            val file = getModelFile(model.filename)
            model.copy(
                isDownloaded = file.exists(),
                fileSizeBytes = if (file.exists()) file.length() else 0
            )
        }
        _availableModels.value = models
        val savedId = AiPreferences.getSelectedModelId()
        if (savedId != null && models.any { it.id == savedId && it.isDownloaded }) {
            _currentModelId.value = savedId
        }
    }

    fun getModelFile(filename: String): File {
        if (!::modelsDir.isInitialized) throw IllegalStateException("ModelManager not initialized")
        return File(modelsDir, filename)
    }

    fun getCurrentModel(): ModelInfo? {
        val id = _currentModelId.value ?: return null
        return _availableModels.value.find { it.id == id }
    }

    suspend fun downloadModel(modelId: String): Boolean = withContext(Dispatchers.IO) {
        if (_isLoading.value) return@withContext false
        val model = _availableModels.value.find { it.id == modelId } ?: return@withContext false
        _isLoading.value = true
        try {
            val url = URL(model.uri)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("User-Agent", "NeoCatroid-AI-Agent")
            }
            connection.connect()
            val totalSize = connection.contentLengthLong
            val inputStream = connection.inputStream
            val outputFile = getModelFile(model.filename)
            inputStream.use { stream ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    val startTime = System.currentTimeMillis()
                    var lastSpeedUpdate = 0L
                    var lastBytesRead = 0L

                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (totalSize > 0) ((totalRead * 100) / totalSize).toInt() else -1
                        _downloadProgress.value = Pair(modelId, progress)

                        val now = System.currentTimeMillis()
                        val elapsed = now - startTime
                        if (elapsed - lastSpeedUpdate > 500 && elapsed > 0) {
                            val bytesThisWindow = totalRead - lastBytesRead
                            val speedBps = (bytesThisWindow * 1000 / (elapsed - lastSpeedUpdate)).coerceAtLeast(1)
                            val speedStr = formatSpeed(speedBps)
                            _downloadSpeed.value = speedStr
                            lastSpeedUpdate = elapsed
                            lastBytesRead = totalRead

                            _detailedProgress.value = DownloadState(
                                modelId = modelId,
                                progress = progress,
                                speedBytesPerSec = speedBps,
                                downloadedBytes = totalRead,
                                totalBytes = totalSize
                            )
                        }
                    }
                }
            }
            refreshModels()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            _isLoading.value = false
            _downloadProgress.value = null
            _downloadSpeed.value = null
            _detailedProgress.value = null
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec > 1_000_000 -> "%.1f MB/s".format(bytesPerSec / 1_000_000.0)
            bytesPerSec > 1_000 -> "%.0f KB/s".format(bytesPerSec / 1_000.0)
            else -> "$bytesPerSec B/s"
        }
    }

    suspend fun loadCustomModelFile(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.extension.equals("gguf", ignoreCase = true)) {
            return@withContext false
        }
        val customId = "custom_${file.nameWithoutExtension}"
        val customModel = ModelInfo(
            id = customId,
            name = file.nameWithoutExtension,
            provider = "Custom",
            size = ModelSize.SIZE_CUSTOM,
            uri = file.absolutePath,
            filename = file.name,
            description = "Custom GGUF model: ${file.name}",
            isDownloaded = true,
            fileSizeBytes = file.length()
        )
        _availableModels.value = _availableModels.value.filter { it.id != customId } + customModel
        _currentModelId.value = customId
        AiPreferences.setSelectedModelId(customId)
        val nCtx = AiPreferences.getMaxContext()
        ModelRuntime.loadModel(file, nCtx)
    }

    suspend fun deleteModel(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val model = _availableModels.value.find { it.id == modelId } ?: return@withContext false
        val file = getModelFile(model.filename)
        val deleted = file.delete()
        if (deleted) {
            if (_currentModelId.value == modelId) {
                _currentModelId.value = null
                AiPreferences.setSelectedModelId(null)
            }
            refreshModels()
        }
        deleted
    }

    suspend fun loadModel(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val model = _availableModels.value.find { it.id == modelId && it.isDownloaded } ?: return@withContext false
        _currentModelId.value = modelId
        AiPreferences.setSelectedModelId(modelId)
        val nCtx = AiPreferences.getMaxContext()
        ModelRuntime.loadModel(getModelFile(model.filename), nCtx)
    }

    fun unloadModel() {
        ModelRuntime.unloadModel()
        _currentModelId.value = null
        AiPreferences.setSelectedModelId(null)
    }
}
