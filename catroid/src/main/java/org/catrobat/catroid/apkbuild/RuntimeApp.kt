package org.catrobat.catroid.apkbuild

import android.app.Application
import android.util.Log
import org.catrobat.catroid.utils.NativeLibraryManager

/**
 * Минимальный Application для рантайм-APK (baked project).
 * Не запускает редактор, плагины, Firebase и т.д.
 * Только инициализирует NativeLibraryManager для LunoScript.
 */
class RuntimeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("RuntimeApp", "Runtime APK starting...")
        NativeLibraryManager.initialize()
    }
}