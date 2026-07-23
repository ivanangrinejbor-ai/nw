package org.catrobat.catroid.ai.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.catrobat.catroid.R
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


class ModelDownloadService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID)
        val url = intent?.getStringExtra(EXTRA_URL)
        val filename = intent?.getStringExtra(EXTRA_FILENAME)
        val displayName = intent?.getStringExtra(EXTRA_NAME) ?: filename ?: ""

        if (modelId == null || url == null || filename == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification(displayName, 0))
        ModelManager.onDownloadStarted()

        scope.launch {
            var success = false
            try {
                success = runDownload(modelId, url, filename, displayName)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ModelManager.onDownloadFinished()
                showResultNotification(displayName, success)
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun runDownload(
        modelId: String,
        url: String,
        filename: String,
        displayName: String
    ): Boolean {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30000
            readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NeoCatroid-AI-Downloader")
        }
        connection.connect()
        val totalSize = connection.contentLengthLong
        val outputFile = ModelManager.getModelFile(filename)

        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                var totalRead = 0L
                val startTime = System.currentTimeMillis()
                var lastUpdate = 0L
                var lastBytes = 0L
                var lastNotifiedPct = -1

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    totalRead += read
                    val progress = if (totalSize > 0) ((totalRead * 100) / totalSize).toInt() else -1

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 400) {
                        val windowMs = (now - startTime - lastUpdate).coerceAtLeast(1)
                        val speedBps = ((totalRead - lastBytes) * 1000 / windowMs).coerceAtLeast(0)
                        lastUpdate = now - startTime
                        lastBytes = totalRead

                        ModelManager.publishProgress(
                            ModelManager.DownloadState(
                                modelId = modelId,
                                progress = progress,
                                speedBytesPerSec = speedBps,
                                downloadedBytes = totalRead,
                                totalBytes = totalSize
                            )
                        )
                        if (progress >= 0 && progress != lastNotifiedPct) {
                            lastNotifiedPct = progress
                            updateNotification(displayName, progress)
                        }
                    }
                }
            }
        }
        return outputFile.exists() && outputFile.length() > 0
    }

    private fun channel(): String {
        val chanId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(chanId) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        chanId,
                        getString(R.string.ai_model_download_channel),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
        return chanId
    }

    private fun buildNotification(name: String, progress: Int): Notification {
        val builder = NotificationCompat.Builder(this, channel())
            .setContentTitle(getString(R.string.ai_agent_downloading, name))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (progress in 0..100) {
            builder.setProgress(100, progress, false)
            builder.setContentText("$progress%")
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun updateNotification(name: String, progress: Int) {
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        mgr.notify(NOTIF_ID, buildNotification(name, progress))
    }

    private fun showResultNotification(name: String, success: Boolean) {
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        val text = getString(
            if (success) R.string.ai_agent_download_complete else R.string.ai_agent_download_failed,
            name
        )
        val notification = NotificationCompat.Builder(this, channel())
            .setContentTitle(name)
            .setContentText(text)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        mgr.notify(NOTIF_ID + 1, notification)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        const val EXTRA_MODEL_ID = "extra_model_id"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILENAME = "extra_filename"
        const val EXTRA_NAME = "extra_name"

        private const val CHANNEL_ID = "ai_model_download"
        private const val NOTIF_ID = 9101
    }
}
