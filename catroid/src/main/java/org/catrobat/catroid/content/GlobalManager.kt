package org.catrobat.catroid.content

import com.danvexteam.lunoscript_annotations.LunoClass
import java.util.concurrent.atomic.AtomicBoolean

@LunoClass
class GlobalManager {
    companion object {
        private val _stopSounds = AtomicBoolean(true)
        private val _saveScenes = AtomicBoolean(true)
        private val _preloadProject = AtomicBoolean(false)

        /**
         * Мастер-громкость игры (0..100). null = не активна.
         * Когда задана, все остальные блоки громкости игнорируются.
         */
        @Volatile
        @JvmStatic
        var gameVolume: Int? = null

        @JvmStatic
        fun getInstance(): GlobalManager = GlobalManager()

        // === Scene tracking (для сенсоров глобальной сцены) ===

        /** Имя текущей активной сцены (для сенсора CURRENT_SCENE_NAME). */
        @Volatile
        @JvmStatic
        var currentSceneName: String = ""

        /** uptimeMillis момента запуска текущей сцены (для сенсора SCENE_TIME). */
        @Volatile
        @JvmStatic
        var currentSceneStartUptimeMs: Long = 0L

        /** Счётчики запусков каждой сцены по имени (для формулы SCENE_LAUNCH_COUNT). */
        @JvmStatic
        val sceneLaunchCounts: java.util.concurrent.ConcurrentHashMap<String, Int> =
            java.util.concurrent.ConcurrentHashMap()

        /** Стек предыдущих сцен для блока "вернуться назад". */
        @JvmStatic
        val sceneBackStack: java.util.ArrayDeque<String> = java.util.ArrayDeque()

        /** true = следующий переход сцены НЕ пушит текущую в back stack (для "назад"). */
        @Volatile
        @JvmStatic
        var suppressNextBackStackPush: Boolean = false

        /** Вызывается при старте сцены: обновляет имя, время, счётчик. */
        @JvmStatic
        fun onSceneStarted(sceneName: String?) {
            val name = sceneName ?: return
            currentSceneName = name
            currentSceneStartUptimeMs = android.os.SystemClock.uptimeMillis()
            sceneLaunchCounts[name] = (sceneLaunchCounts[name] ?: 0) + 1
        }

        /** Секунды с момента запуска текущей сцены. */
        @JvmStatic
        fun sceneTimeSeconds(): Double {
            if (currentSceneStartUptimeMs == 0L) return 0.0
            return (android.os.SystemClock.uptimeMillis() - currentSceneStartUptimeMs) / 1000.0
        }

        /** Сброс всего трекинга сцен (при перезапуске проекта). */
        @JvmStatic
        fun resetSceneTracking() {
            currentSceneName = ""
            currentSceneStartUptimeMs = 0L
            sceneLaunchCounts.clear()
            sceneBackStack.clear()
        }

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