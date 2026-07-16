package org.catrobat.catroid.apkbuildV3

/**
 * Callback interface for receiving build progress updates.
 * Designed to be called from background threads — implementors must
 * post to the appropriate thread (e.g. Main Looper) themselves.
 */
fun interface BuildProgressListener {
    /**
     * @param progress  Progress in percent (0.0 .. 100.0)
     * @param stage     Human-readable description of the current build stage
     */
    fun onProgress(progress: Float, stage: String)
}
