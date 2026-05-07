package org.catrobat.catroid.content.actions

import android.os.CountDownTimer
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.AudioAnalyzerController
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.VolumeManager
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class ListenMicroAction : TemporalAction() {
    private var audioAnalyzer = AudioAnalyzerController(CatroidApplication.getAppContext())
    private var countDownTimer: CountDownTimer? = null

    var scope: Scope? = null
    var time: Formula? = null

    override fun begin() {
        super.begin()
        Log.d("ListenMicro", "Started")

        val updateInterval = (time?.interpretObject(scope)?.toString()?.toLongOrNull() ?: 100L)
        val activity = StageActivity.activeStageActivity.get()

        activity?.runOnUiThread {
            audioAnalyzer.start()

            countDownTimer = object : CountDownTimer(60_000, updateInterval) {
                override fun onTick(millisUntilFinished: Long) {
                    VolumeManager.volume = audioAnalyzer.currentVolume
                    VolumeManager.frequency = audioAnalyzer.currentFrequency
                }

                override fun onFinish() {
                    audioAnalyzer.stop()
                    Log.d("ListenMicro", "Timer Ended")
                }
            }.apply {
                start()
            }
        }
    }

    override fun update(percent: Float) {

    }

    fun stop() {
        countDownTimer?.cancel()
        audioAnalyzer.stop()
    }
}