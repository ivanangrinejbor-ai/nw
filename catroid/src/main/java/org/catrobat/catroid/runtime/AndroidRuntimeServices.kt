/*
 * NeoCatroid — Android implementation of RuntimeServices.
 *
 * Delegates to the existing Android managers/singletons. Pure additive code:
 * no existing call sites are changed by adding this file.
 */

package org.catrobat.catroid.runtime

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import org.catrobat.catroid.formulaeditor.SensorHandler
import org.catrobat.catroid.utils.VibrationManager

class AndroidRuntimeServices(private val context: Context) : RuntimeServices {

    override fun getExternalStorageDir(): String =
        Environment.getExternalStorageDirectory().absolutePath

    override fun getDownloadsDir(): String =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

    override fun postToMainThread(runnable: Runnable) {
        Handler(Looper.getMainLooper()).post(runnable)
    }

    override fun isGpsAvailable(): Boolean = SensorHandler.gpsAvailable()

    private val vibrationManager: VibrationManager? by lazy {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.let {
            VibrationManager().apply { vibration = it }
        }
    }

    override fun hasVibrator(): Boolean = vibrationManager != null

    override fun vibrate(durationMs: Long) {
        vibrationManager?.vibrateFor(durationMs)
    }

    override fun postDelayed(runnable: Runnable, delayMs: Long) {
        Handler(Looper.getMainLooper()).postDelayed(runnable, delayMs)
    }
}
