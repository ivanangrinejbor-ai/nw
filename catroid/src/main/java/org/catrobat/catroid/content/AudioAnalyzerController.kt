package org.catrobat.catroid.content

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat

class AudioAnalyzerController(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var analyzeThread: Thread? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    var currentVolume: Int = 0
        private set

    var currentFrequency: Float = 0f
        private set

    fun start() {
        if (isRecording) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Нет разрешения на использование микрофона. Поназапрещали всякого, твари :(")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            audioRecord?.startRecording()
            isRecording = true

            analyzeThread = Thread { analyzeAudioStream() }
            analyzeThread?.start()

            Log.d(TAG, "Microphone started listening")
        } catch (e: SecurityException) {
            Log.e(TAG, "Ошибка прав доступа: ${e.message}")
        }
    }

    private fun analyzeAudioStream() {
        val buffer = ShortArray(bufferSize)

        while (isRecording) {
            val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (readSize > 0) {
                var maxAmplitude = 0
                for (i in 0 until readSize) {
                    val amplitude = Math.abs(buffer[i].toInt())
                    if (amplitude > maxAmplitude) {
                        maxAmplitude = amplitude
                    }
                }
                currentVolume = maxAmplitude

                var zeroCrossings = 0
                for (i in 1 until readSize) {
                    if (buffer[i - 1] < 0 && buffer[i] >= 0) {
                        zeroCrossings++
                    }
                }

                if (zeroCrossings > 0) {
                    currentFrequency = (zeroCrossings.toFloat() * sampleRate) / readSize
                } else {
                    currentFrequency = 0f
                }

                if (currentVolume < 500) {
                    currentFrequency = 0f
                }
            }
        }
    }

    fun stop() {
        isRecording = false
        analyzeThread?.join(500)
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        currentVolume = 0
        currentFrequency = 0f
        Log.d(TAG, "Microphone stopped")
    }

    private companion object {
        private val TAG = AudioAnalyzerController::class.java.name
    }
}