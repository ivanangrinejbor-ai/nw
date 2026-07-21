package org.catrobat.catroid.apkbuild

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ResultReceiver
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.catrobat.catroid.R
import java.io.File

class ApkBuildService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())

        val projectDir = File(intent.getStringExtra(EXTRA_PROJECT_DIR) ?: "")
        val config = intent.getParcelableExtraCompat<BakedApkBuilder.ApkConfig>(EXTRA_CONFIG)
        val receiver = intent.getParcelableExtraCompat<ResultReceiver>(EXTRA_RECEIVER)

        if (config == null || receiver == null) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            try {
                val result = BakedApkBuilder.build(this@ApkBuildService, projectDir, config) { progress ->
                    receiver.send(WHAT_PROGRESS, Bundle().apply { putString(KEY_PROGRESS, progress) })
                }
                val rb = Bundle()
                when (result) {
                    is BakedApkBuilder.BuildResult.Success -> {
                        rb.putBoolean(KEY_SUCCESS, true)
                        rb.putString(KEY_APK_PATH, result.apkFile.absolutePath)
                    }
                    is BakedApkBuilder.BuildResult.Error -> {
                        rb.putBoolean(KEY_SUCCESS, false)
                        rb.putString(KEY_ERROR, result.message)
                    }
                }
                receiver.send(WHAT_RESULT, rb)
            } catch (e: Throwable) {
                receiver.send(
                    WHAT_RESULT,
                    Bundle().apply {
                        putBoolean(KEY_SUCCESS, false)
                        putString(KEY_ERROR, e.message ?: "Unknown error")
                    }
                )
            } finally {
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val chanId = "apk_build_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(chanId) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(chanId, "APK Build", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        return NotificationCompat.Builder(this, chanId)
            .setContentTitle(getString(R.string.build_apk_title))
            .setContentText(getString(R.string.build_apk_progress))
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .build()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        const val EXTRA_PROJECT_DIR = "project_dir"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_RECEIVER = "receiver"

        const val WHAT_PROGRESS = 1
        const val WHAT_RESULT = 2

        const val KEY_PROGRESS = "progress"
        const val KEY_SUCCESS = "success"
        const val KEY_APK_PATH = "apk_path"
        const val KEY_ERROR = "error"

        private const val NOTIF_ID = 9001
    }
}

private inline fun <reified T> Intent.getParcelableExtraCompat(name: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        getParcelableExtra(name) as? T
    }
