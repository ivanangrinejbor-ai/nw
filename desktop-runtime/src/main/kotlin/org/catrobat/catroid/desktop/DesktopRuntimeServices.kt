package org.catrobat.catroid.desktop

import com.badlogic.gdx.Gdx
import org.catrobat.catroid.runtime.RuntimeServices
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DesktopRuntimeServices(
    private val workingDir: File = File(System.getProperty("user.dir"))
) : RuntimeServices {

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Desktop-Scheduler-Worker").apply { isDaemon = true }
    }

    override fun getExternalStorageDir(): String {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "NeoCatroid")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }

    override fun getDownloadsDir(): String {
        val userHome = System.getProperty("user.home")
        val downloads = File(userHome, "Downloads")
        if (downloads.exists()) {
            return downloads.absolutePath
        }
        return workingDir.absolutePath
    }

    override fun postToMainThread(runnable: Runnable) {
        if (Gdx.app != null) {
            Gdx.app.postRunnable(runnable)
        } else {
            runnable.run()
        }
    }

    override fun postDelayed(runnable: Runnable, delayMs: Long) {
        scheduler.schedule({
            postToMainThread(runnable)
        }, delayMs, TimeUnit.MILLISECONDS)
    }

    override fun isGpsAvailable(): Boolean = false

    override fun hasVibrator(): Boolean = false

    override fun vibrate(durationMs: Long) {}
}
