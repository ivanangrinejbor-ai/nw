package org.catrobat.catroid.audio

/**
 * Platform-independent audio surface used by sound bricks.
 *
 * Mirrors the Sprite-free primitives of the Android [org.catrobat.catroid.io.SoundManager]
 * so the desktop player can supply an alternative implementation (e.g. OpenAL)
 * without touching the action classes.
 */
interface AudioService {
    fun setVolume(volume: Float)
    fun getVolume(): Float
    fun setPan(pan: Float)
    fun getPan(): Float
    fun setPitch(pitch: Float)
    fun getPitch(): Float
    fun stopAllSounds()
    fun clear()
    fun pause()
    fun resume()

    fun playSoundFile(filePath: String, spriteName: String)
    fun playSoundFileWithStartTime(filePath: String, spriteName: String, startTime: Int)
    fun stopSoundInSprite(filePath: String, spriteName: String)
    fun setVolumeForSound(filePath: String, spriteName: String, volume: Float)

    fun playTone(samples: ShortArray, sampleRate: Int)
    fun stopTone()
    fun setEqualizerBand(band: Int, gain: Short)
    fun isSoundPlaying(soundFilePath: String, spriteName: String): Boolean
}
