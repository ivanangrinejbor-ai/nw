package org.catrobat.catroid.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.io.SoundManager
import org.catrobat.catroid.pocketmusic.mididriver.MidiSoundManager

class AndroidAudioService : AudioService {

    private val soundManager = SoundManager.getInstance()
    private val midiSoundManager = MidiSoundManager.getInstance()

    private var toneTrack: AudioTrack? = null
    private var equalizer: Equalizer? = null

    override fun setVolume(volume: Float) = soundManager.setVolume(volume)
    override fun getVolume(): Float = soundManager.getVolume()
    override fun setPan(pan: Float) = soundManager.setPan(pan)
    override fun getPan(): Float = soundManager.getPan()
    override fun setPitch(pitch: Float) = soundManager.setPitch(pitch)
    override fun getPitch(): Float = soundManager.getPitch()
    override fun stopAllSounds() = soundManager.stopAllSounds()
    override fun clear() = soundManager.clear()
    override fun pause() = soundManager.pause()
    override fun resume() = soundManager.resume()

    override fun playSoundFile(filePath: String, spriteName: String) {
        resolveSprite(spriteName)?.let { soundManager.playSoundFile(filePath, it) }
    }

    override fun playSoundFileWithStartTime(filePath: String, spriteName: String, startTime: Int) {
        resolveSprite(spriteName)?.let { soundManager.playSoundFileWithStartTime(filePath, it, startTime) }
    }

    override fun stopSoundInSprite(filePath: String, spriteName: String) {
        resolveSprite(spriteName)?.let { soundManager.stopSameSoundInSprite(filePath, it) }
    }

    override fun setVolumeForSound(filePath: String, spriteName: String, volume: Float) {
        resolveSprite(spriteName)?.let { soundManager.setVolumeForSound(filePath, it, volume) }
    }

    override fun playTone(samples: ShortArray, sampleRate: Int) {
        try {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            toneTrack?.let { try { it.stop(); it.release() } catch (_: Exception) {} }
            toneTrack = AudioTrack.Builder()
                .setAudioAttributes(attr)
                .setAudioFormat(format)
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            toneTrack?.write(samples, 0, samples.size)
            toneTrack?.play()
        } catch (e: Exception) {
            android.util.Log.d("AndroidAudioService", "Failed to play tone", e)
        }
    }

    override fun stopTone() {
        try {
            toneTrack?.stop()
            toneTrack?.release()
        } catch (e: Exception) {
            android.util.Log.d("AndroidAudioService", "Failed to stop tone", e)
        }
        toneTrack = null
    }

    override fun setEqualizerBand(band: Int, gain: Short) {
        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, 0)
            }
            equalizer?.let { eq ->
                val numberOfBands = eq.numberOfBands
                if (band in 0 until numberOfBands) {
                    eq.setBandLevel(band.toShort(), gain)
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("AndroidAudioService", "Equalizer failed", e)
        }
    }

    override fun isSoundPlaying(soundFilePath: String, spriteName: String): Boolean {
        val sprite = resolveSprite(spriteName) ?: return false
        val mediaPlaying = soundManager.getMediaPlayers().any {
            it.isPlaying() && it.getStartedBySprite() == sprite && it.getPathToSoundFile() == soundFilePath
        }
        val midiPlaying = midiSoundManager.isSoundInSpritePlaying(sprite, soundFilePath)
        return mediaPlaying || midiPlaying
    }

    internal fun resolveSprite(name: String): Sprite? {
        val project = ProjectManager.getInstance().currentProject ?: return null
        return project.getSpriteListWithClones().firstOrNull { it.name == name }
    }
}
