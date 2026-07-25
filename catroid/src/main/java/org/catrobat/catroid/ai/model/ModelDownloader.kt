package org.catrobat.catroid.ai.model

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    /** Per-model Job so cancel() actually stops the download coroutine. */
    private val activeJobs = mutableMapOf<String, Job>()

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
        // Do not start a second download for the same model
        synchronized(activeJobs) {
            if (activeJobs[modelId]?.isActive == true) return
        }

        val job = scope.launch {
            updateState(modelId) { it.copy(isRunning = true, progress = 0, error = null, isComplete = false) }
            var totalSize = -1L
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "NeoCatroid-AI-Downloader")
                }
                connection.connect()
                totalSize = connection.contentLengthLong
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
                // Clean up partial/corrupt file so refreshModels() won't show it as downloaded
                if (destination.exists() && (totalSize <= 0 || destination.length() < totalSize)) {
                    destination.delete()
                }
                val isCancelled = e is CancellationException
                updateState(modelId) {
                    it.copy(
                        isRunning = false,
                        isComplete = false,
                        error = if (isCancelled) null else e.message
                    )
                }
            } finally {
                synchronized(activeJobs) { activeJobs.remove(modelId) }
            }
        }
        synchronized(activeJobs) { activeJobs[modelId] = job }
    }

    /** Cancels the running download coroutine (not just the state). */
    fun cancel(modelId: String) {
        synchronized(activeJobs) { activeJobs.remove(modelId) }?.cancel()
        updateState(modelId) { it.copy(isRunning = false, isComplete = false, progress = 0, error = null) }
    }

    private fun updateState(modelId: String, update: (DownloadState) -> DownloadState) {
        val current = _downloads.value.toMutableMap()
        val existing = current[modelId] ?: DownloadState(modelId)
        current[modelId] = update(existing)
        _downloads.value = current
    }
}
