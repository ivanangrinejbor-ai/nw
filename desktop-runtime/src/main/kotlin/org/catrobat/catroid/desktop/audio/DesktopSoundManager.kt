package org.catrobat.catroid.desktop.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DesktopSoundManager(
    private val soundsDir: File
) {
    private val loadedSounds = ConcurrentHashMap<String, Sound>()
    private val loadedMusic = ConcurrentHashMap<String, Music>()
    private var globalVolume: Float = 1.0f

    fun playSound(fileName: String, volume: Float = 1.0f, loop: Boolean = false): Long {
        val soundFile = File(soundsDir, fileName)
        if (!soundFile.exists()) {
            Gdx.app.error("DesktopSoundManager", "Sound file not found: ${soundFile.absolutePath}")
            return -1L
        }

        val sound = loadedSounds.computeIfAbsent(fileName) {
            Gdx.audio.newSound(Gdx.files.absolute(soundFile.absolutePath))
        }

        val actualVolume = (volume * globalVolume).coerceIn(0.0f, 1.0f)
        return if (loop) {
            sound.loop(actualVolume)
        } else {
            sound.play(actualVolume)
        }
    }

    fun playMusic(fileName: String, loop: Boolean = true, volume: Float = 1.0f) {
        val musicFile = File(soundsDir, fileName)
        if (!musicFile.exists()) {
            Gdx.app.error("DesktopSoundManager", "Music file not found: ${musicFile.absolutePath}")
            return
        }

        val music = loadedMusic.computeIfAbsent(fileName) {
            Gdx.audio.newMusic(Gdx.files.absolute(musicFile.absolutePath))
        }

        music.isLooping = loop
        music.volume = (volume * globalVolume).coerceIn(0.0f, 1.0f)
        music.play()
    }

    fun stopAllSounds() {
        loadedSounds.values.forEach { it.stop() }
        loadedMusic.values.forEach { it.stop() }
    }

    fun setGlobalVolume(volume: Float) {
        globalVolume = volume.coerceIn(0.0f, 1.0f)
        loadedMusic.values.forEach { it.volume = globalVolume }
    }

    fun dispose() {
        loadedSounds.values.forEach { it.dispose() }
        loadedSounds.clear()
        loadedMusic.values.forEach { it.dispose() }
        loadedMusic.clear()
    }
}
