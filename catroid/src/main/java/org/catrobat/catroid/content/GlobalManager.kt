package org.catrobat.catroid.content

import com.danvexteam.lunoscript_annotations.LunoClass
import java.util.concurrent.atomic.AtomicBoolean

@LunoClass
class GlobalManager {
    companion object {
        private val _stopSounds = AtomicBoolean(true)
        private val _saveScenes = AtomicBoolean(true)
        private val _preloadProject = AtomicBoolean(false)

        @Volatile
        @JvmStatic
        var gameVolume: Int? = null

        @JvmStatic
        fun getInstance(): GlobalManager = GlobalManager()

        @Volatile
        @JvmStatic
        var currentSceneName: String = ""

        @Volatile
        @JvmStatic
        var currentSceneStartUptimeMs: Long = 0L

        @JvmStatic
        val sceneLaunchCounts: java.util.concurrent.ConcurrentHashMap<String, Int> =
            java.util.concurrent.ConcurrentHashMap()

        @JvmStatic
        val sceneBackStack: java.util.ArrayDeque<String> = java.util.ArrayDeque()

        @Volatile
        @JvmStatic
        var suppressNextBackStackPush: Boolean = false

        @JvmStatic
        fun onSceneStarted(sceneName: String?) {
            val name = sceneName ?: return
            currentSceneName = name
            currentSceneStartUptimeMs = android.os.SystemClock.uptimeMillis()
            sceneLaunchCounts[name] = (sceneLaunchCounts[name] ?: 0) + 1
        }

        @JvmStatic
        fun sceneTimeSeconds(): Double {
            if (currentSceneStartUptimeMs == 0L) return 0.0
            return (android.os.SystemClock.uptimeMillis() - currentSceneStartUptimeMs) / 1000.0
        }

        @JvmStatic
        fun resetSceneTracking() {
            currentSceneName = ""
            currentSceneStartUptimeMs = 0L
            sceneLaunchCounts.clear()
            sceneBackStack.clear()
        }

        @JvmStatic
        var stopSounds: Boolean
            get() = _stopSounds.get()
            set(value) = _stopSounds.set(value)

        var saveScenes: Boolean
            get() = _saveScenes.get()
            set(value) = _saveScenes.set(value)

        var preloadProject: Boolean
            get() = _preloadProject.get()
            set(value) = _preloadProject.set(value)
    }
}