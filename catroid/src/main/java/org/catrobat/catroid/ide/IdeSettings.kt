package org.catrobat.catroid.ide

import android.content.Context
import java.io.File

object IdeSettings {
    fun getSdkDir(context: Context): File {
        val dir = File(context.filesDir, "AndroidSDK")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getAndroidJar(context: Context, apiLevel: Int): File {
        return File(getSdkDir(context), "platforms/android-$apiLevel/android.jar")
    }
}
