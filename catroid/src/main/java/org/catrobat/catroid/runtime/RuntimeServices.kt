/*
 * NeoCatroid — RuntimeServices contract.
 *
 * Platform-agnostic contract for the few Android services the brick/action
 * runtime depends on. `AndroidRuntimeServices` implements it on Android;
 * a future `:desktop-runtime` will provide a desktop implementation.
 *
 * This file is pure Kotlin (no android.* in the interface signatures) so it can
 * be shared between the Android app and the desktop player.
 */

package org.catrobat.catroid.runtime

import java.lang.Runnable

interface RuntimeServices {
    /** Absolute path to the external storage root (Android) / user home (desktop). */
    fun getExternalStorageDir(): String

    /** Post a task onto the platform main/UI thread. */
    fun postToMainThread(runnable: Runnable)

    /** Post a task onto the platform main/UI thread after the given delay (ms). */
    fun postDelayed(runnable: Runnable, delayMs: Long)

    /** Whether a GPS/location provider is available. */
    fun isGpsAvailable(): Boolean

    /** Whether the device can vibrate. */
    fun hasVibrator(): Boolean

    /** Vibrate for the given duration in milliseconds (no-op if unsupported). */
    fun vibrate(durationMs: Long)
}
