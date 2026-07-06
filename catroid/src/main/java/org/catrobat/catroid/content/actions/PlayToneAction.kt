package org.catrobat.catroid.content.actions

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException

class PlayToneAction : TemporalAction() {
    var scope: Scope? = null
    var frequency: Formula? = null
    var duration: Formula? = null
    private var audioTrack: AudioTrack? = null

    override fun begin() {
        try {
            val freq = frequency?.interpretFloat(scope) ?: 440f
            val dur = duration?.interpretFloat(scope) ?: 1f
            super.setDuration(dur)

            val sampleRate = 44100
            val numSamples = (sampleRate * dur).toInt()
            if (numSamples <= 0) return
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val sample = (Math.sin(2.0 * Math.PI * freq / sampleRate * i) * Short.MAX_VALUE).toInt().toShort()
                samples[i] = sample
            }

            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attr)
                .setAudioFormat(format)
                .setBufferSizeInBytes(numSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.write(samples, 0, numSamples)
            audioTrack?.play()
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed", e)
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Failed to play tone", e)
        }
    }

    override fun update(percent: Float) {
    }

    override fun end() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Failed to stop tone", e)
        }
        audioTrack = null
    }
}
