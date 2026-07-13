package org.catrobat.catroid.runtime

import com.badlogic.gdx.Gdx
import java.io.File
import java.lang.Runnable

/**
 * Desktop (Windows) implementation of [RuntimeServices] using the standard JDK.
 * Storage roots map to the user home / Downloads folder; scheduling uses plain threads.
 */
class DesktopRuntimeServices : RuntimeServices {
    override fun getExternalStorageDir(): String =
        System.getProperty("user.home") ?: "."

    override fun getDownloadsDir(): String =
        File(System.getProperty("user.home"), "Downloads").absolutePath

    override fun postToMainThread(runnable: Runnable) {
        val app = Gdx.app
        if (app != null) {
            app.postRunnable(runnable)
        } else {
            Thread(runnable).start()
        }
    }

    override fun postDelayed(runnable: Runnable, delayMs: Long) {
        Thread {
            try {
                Thread.sleep(delayMs)
            } catch (_: InterruptedException) {
                // cancelled
            }
            val app = Gdx.app
            if (app != null) {
                app.postRunnable(runnable)
            } else {
                runnable.run()
            }
        }.start()
    }

    override fun isGpsAvailable(): Boolean = false

    override fun hasVibrator(): Boolean = false

    override fun vibrate(durationMs: Long) {
        // No vibrator on desktop; intentionally a no-op.
    }
}
