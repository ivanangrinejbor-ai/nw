package org.catrobat.catroid.desktop.speech

import java.util.concurrent.Executors

class DesktopSpeechSynthesizer {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Desktop-TTS-Worker").apply { isDaemon = true }
    }

    fun speak(text: String, rate: Float = 1.0f) {
        val cleanText = text.replace("\"", "\\\"").replace("'", "\\'")
        executor.submit {
            try {
                val psCommand = "Add-Type -AssemblyName System.Speech; " +
                        "\$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "\$synth.Rate = 0; " +
                        "\$synth.Speak('$cleanText');"

                val process = ProcessBuilder("powershell", "-NoProfile", "-Command", psCommand)
                    .redirectErrorStream(true)
                    .start()
                process.waitFor()
            } catch (e: Exception) {
                System.err.println("[DesktopTTS] Error speaking text: ${e.message}")
            }
        }
    }
}
