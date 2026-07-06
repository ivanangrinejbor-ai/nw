package org.catrobat.catroid.apkbuild

import android.app.Application
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.koin.myModules
import org.catrobat.catroid.utils.NativeLibraryManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module

/**
 * Минимальный Application для рантайм-APK (baked project).
 * Не запускает редактор, плагины, Firebase и т.д.
 * Только инициализирует NativeLibraryManager, контекст и Koin для LunoScript.
 */
class RuntimeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("RuntimeApp", "Runtime APK starting...")
        CatroidApplication.setAppContext(applicationContext)
        try {
            startKoin {
                androidContext(applicationContext)
                androidLogger(Level.ERROR)
                modules(myModules)
            }
        } catch (e: Exception) {
            Log.w("RuntimeApp", "Koin init failed (non-fatal): ${e.message}")
        }
        NativeLibraryManager.initialize()
    }
}