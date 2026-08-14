package org.catrobat.catroid.telemetry

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.catrobat.catroid.BuildConfig
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

object TelemetryManager {

    private const val TAG = "TelemetryManager"
    private const val PREFS_NAME = "telemetry_prefs"
    private const val KEY_INSTALL_UUID = "telemetry_id"
    private const val COLLECTION_NAME = "telemetry"
    private const val TELEMETRY_APP_NAME = "telemetry"

    private const val FIREBASE_APPLICATION_ID = "1:910379908682:android:27ae2aa6d6fa5f2a2270b3"
    private const val FIREBASE_API_KEY = "AIzaSyCtR88-Jlj-7Vm63g6dBA7lmgp3xmmudZY"
    private const val FIREBASE_PROJECT_ID = "privacy-neocatroid"
    private const val FIREBASE_GCM_SENDER_ID = "910379908682"

    private var launchStartElapsedMs = 0L
    private val sentForProcess = AtomicBoolean(false)

    @JvmStatic
    fun recordLaunchStart() {
        launchStartElapsedMs = SystemClock.elapsedRealtime()
    }

    @JvmStatic
    fun onLaunchCompleted(context: Context) {
        if (!sentForProcess.compareAndSet(false, true)) {
            return
        }
        val launchDurationMs = if (launchStartElapsedMs > 0L) {
            SystemClock.elapsedRealtime() - launchStartElapsedMs
        } else {
            0L
        }
        sendTelemetry(context, launchDurationMs)
    }

    private fun sendTelemetry(context: Context, launchDurationMs: Long) {
        try {
            val firestore = getTelemetryFirestore(context) ?: return
            val uuid = getOrCreateInstallUuid(context)
            val data = buildTelemetryData(context, launchDurationMs)
            firestore.collection(COLLECTION_NAME)
                .document(uuid)
                .set(data, SetOptions.merge())
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "telemetry send failed", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "telemetry send failed", e)
        }
    }

    internal fun getTelemetryFirestore(context: Context): FirebaseFirestore? {
        val app = getTelemetryApp(context) ?: return null
        return FirebaseFirestore.getInstance(app)
    }

    internal fun getTelemetryApp(context: Context): FirebaseApp? {
        return try {
            FirebaseApp.getApps(context).firstOrNull { it.name == TELEMETRY_APP_NAME }?.let { return it }
            val options = FirebaseOptions.Builder()
                .setApplicationId(FIREBASE_APPLICATION_ID)
                .setApiKey(FIREBASE_API_KEY)
                .setProjectId(FIREBASE_PROJECT_ID)
                .setGcmSenderId(FIREBASE_GCM_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(context, options, TELEMETRY_APP_NAME)
        } catch (e: Exception) {
            Log.w(TAG, "telemetry firebase app init failed", e)
            null
        }
    }

    private fun getOrCreateInstallUuid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_INSTALL_UUID, null)
        if (!existing.isNullOrEmpty()) {
            return existing
        }
        val newUuid = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALL_UUID, newUuid).apply()
        return newUuid
    }

    private fun buildTelemetryData(context: Context, launchDurationMs: Long): Map<String, Any> {
        return linkedMapOf(
            "app_version" to BuildConfig.VERSION_NAME,
            "package_name" to BuildConfig.APPLICATION_ID,
            "android_version" to Build.VERSION.RELEASE,
            "device_model" to Build.MODEL,
            "cpu" to getCpuName(),
            "device_board" to Build.BOARD,
            "screen" to screenResolution(context),
            "launch_duration" to launchDurationMs,
            "last_seen" to LocalDate.now().toString()
        )
    }

    private fun getCpuName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manufacturer = Build.SOC_MANUFACTURER.orEmpty()
            val model = Build.SOC_MODEL.orEmpty()
            listOf(manufacturer, model)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        } else {
            Build.HARDWARE
        }
    }

    private fun screenResolution(context: Context): String {
        @Suppress("DEPRECATION")
        return try {
            val metrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.maximumWindowMetrics.bounds
                "${bounds.width()}x${bounds.height()}"
            } else {
                windowManager.defaultDisplay.getRealMetrics(metrics)
                "${metrics.widthPixels}x${metrics.heightPixels}"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
