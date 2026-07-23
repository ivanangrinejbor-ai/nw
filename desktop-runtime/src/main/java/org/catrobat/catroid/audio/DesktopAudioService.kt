package org.catrobat.catroid.audio

import java.io.File
import java.util.Collections
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

class DesktopAudioService : AudioService {
    private val clips = Collections.synchronizedMap(LinkedHashMap<String, Clip>())
    /**
     * Volume stored as Android-style 0..100 scale.
     * Android SendVolume/ChangeVolume actions use 0..100 range.
     * Internally converted to 0..1 for javax.sound.sampled MASTER_GAIN.
     */
    private var volume = 100f
    private var pan = 0.0f
    // pitch is stored but not applied to Clip playback (java.sound.sampled doesn't support pitch natively)
    private var pitch = 1.0f

    /**
     * Accepts volume in Android scale (0..100) and maps to internal 0..1 range.
     */
    override fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 100f)
        synchronized(clips) { clips.values.forEach { setClipVolume(it) } }
    }

    /**
     * Returns volume in Android scale (0..100) for compatibility with
     * ChangeVolumeByNAction and other Android-ported actions.
     */
    override fun getVolume(): Float = volume

    override fun setPan(p: Float) {
        pan = p.coerceIn(-1f, 1f)
    }

    override fun getPan(): Float = pan

    override fun setPitch(p: Float) {
        pitch = p.coerceIn(0.01f, 16f)
    }

    override fun getPitch(): Float = pitch

    override fun stopAllSounds() {
        synchronized(clips) {
            clips.values.forEach { try { it.stop(); it.close() } catch (_: Exception) { } }
            clips.clear()
        }
    }

    override fun clear() {
        stopAllSounds()
    }

    override fun pause() {
        synchronized(clips) { clips.values.forEach { try { it.stop() } catch (_: Exception) { } } }
    }

    override fun resume() {}

    override fun playSoundFile(filePath: String, spriteName: String) = playFile(filePath)

    override fun playSoundFileWithStartTime(filePath: String, spriteName: String, startTime: Int) =
        playFile(filePath, startTime)

    override fun stopSoundInSprite(filePath: String, spriteName: String) {
        clips[filePath]?.let {
            try { it.stop() } catch (_: Exception) { }
            clips.remove(filePath)
        }
    }

    override fun setVolumeForSound(filePath: String, spriteName: String, volume: Float) {
        clips[filePath]?.let { setClipVolume(it, volume.coerceIn(0f, 100f) / 100f) }
    }

    private fun playFile(filePath: String, startTime: Int = 0) {
        try {
            clips[filePath]?.let { old ->
                try { old.stop(); old.close() } catch (_: Exception) { }
            }
            val clip = AudioSystem.getClip()
            clip.open(AudioSystem.getAudioInputStream(File(filePath)))
            setClipVolume(clip, volume)
            try {
                val panControl = clip.getControl(FloatControl.Type.PAN) as? FloatControl
                if (panControl != null) panControl.value = pan
            } catch (_: Exception) { }
            if (startTime > 0 && clip.format.sampleRate > 0f) {
                clip.framePosition = (startTime * clip.format.sampleRate / 1000).toInt()
            }
            clip.start()
            clips[filePath] = clip
        } catch (_: Exception) {
        }
    }

    /**
     * Accepts volume in 0..100 Android scale and converts to 0..1 for MASTER_GAIN.
     */
    private fun setClipVolume(clip: Clip, v: Float = volume) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val control = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                // Convert Android 0..100 → 0..1 → dB gain
                val normalized = (v.coerceIn(0f, 100f) / 100f).coerceIn(0f, 1f)
                val gain = (kotlin.math.log10(normalized.coerceAtLeast(0.0001f).toDouble()) * 20).toFloat()
                control.value = gain.coerceIn(control.minimum, control.maximum)
            }
        } catch (_: Exception) {
        }
    }

    override fun playTone(samples: ShortArray, sampleRate: Int) {
        try {
            val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val line = AudioSystem.getSourceDataLine(format)
            line.open(format)
            line.start()
            val bytes = ByteArray(samples.size * 2)
            for (i in samples.indices) {
                bytes[i * 2] = (samples[i].toInt() and 0xFF).toByte()
                bytes[i * 2 + 1] = (samples[i].toInt() shr 8 and 0xFF).toByte()
            }
            line.write(bytes, 0, bytes.size)
            line.drain()
            line.close()
        } catch (_: Exception) {
        }
    }

    override fun stopTone() {}

    override fun setEqualizerBand(band: Int, gain: Short) {
    }

    override fun isSoundPlaying(soundFilePath: String, spriteName: String): Boolean =
        clips[soundFilePath]?.isRunning ?: false
}
