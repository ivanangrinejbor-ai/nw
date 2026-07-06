package org.catrobat.catroid.content

import com.danvexteam.lunoscript_annotations.LunoClass
import java.util.concurrent.atomic.AtomicBoolean

@LunoClass
class GlobalManager {
    companion object {
        private val _stopSounds = AtomicBoolean(true)
        private val _saveScenes = AtomicBoolean(true)
        private val _preloadProject = AtomicBoolean(false)

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