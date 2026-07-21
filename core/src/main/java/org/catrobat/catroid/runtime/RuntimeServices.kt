package org.catrobat.catroid.runtime

import java.lang.Runnable

interface RuntimeServices {
    fun getExternalStorageDir(): String
    fun getDownloadsDir(): String
    fun postToMainThread(runnable: Runnable)
    fun postDelayed(runnable: Runnable, delayMs: Long)
    fun isGpsAvailable(): Boolean
    fun hasVibrator(): Boolean
    fun vibrate(durationMs: Long)
}
