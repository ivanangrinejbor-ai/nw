package org.catrobat.catroid.ai.model

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var context: Context? = null

    private val _downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadState>> = _downloads.asStateFlow()

    data class DownloadState(
        val modelId: String,
        val progress: Int = 0,
        val isRunning: Boolean = false,
        val isComplete: Boolean = false,
        val error: String? = null
    )

    fun init(appContext: Context) {
        context = appContext
    }

    fun download(modelId: String, url: String, destination: File) {
        scope.launch {
            updateState(modelId) { it.copy(isRunning = true, progress = 0) }
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", "NeoCatroid-AI-Downloader")
                }
                connection.connect()
                val totalSize = connection.contentLengthLong
                val input = connection.inputStream
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        val pct = if (totalSize > 0) ((totalRead * 100) / totalSize).toInt() else -1
                        updateState(modelId) { it.copy(progress = pct) }
                    }
                }
                input.close()
                updateState(modelId) { it.copy(isRunning = false, isComplete = true, progress = 100) }
            } catch (e: Exception) {
                updateState(modelId) { it.copy(isRunning = false, error = e.message) }
            }
        }
    }

    fun cancel(modelId: String) {
        updateState(modelId) { it.copy(isRunning = false, isComplete = false, progress = 0) }
    }

    private fun updateState(modelId: String, update: (DownloadState) -> DownloadState) {
        val current = _downloads.value.toMutableMap()
        val existing = current[modelId] ?: DownloadState(modelId)
        current[modelId] = update(existing)
        _downloads.value = current
    }
}
