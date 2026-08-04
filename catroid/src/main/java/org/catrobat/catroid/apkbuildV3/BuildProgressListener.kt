package org.catrobat.catroid.apkbuildV3

fun interface BuildProgressListener {
    fun onProgress(progress: Float, stage: String, currentFile: String)
}