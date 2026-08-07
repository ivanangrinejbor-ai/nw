package org.catrobat.catroid.ai.speech

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

object AiVoiceController {

    const val REQUEST_CODE_SPEECH_INPUT = 8842

    fun startVoiceRecognition(activity: Activity) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Произнесите команду для ИИ Агента...")
        }
        try {
            activity.startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (_: Exception) {}
    }

    fun parseSpeechResult(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode != REQUEST_CODE_SPEECH_INPUT || resultCode != Activity.RESULT_OK || data == null) return null
        val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return result?.firstOrNull()
    }
}
