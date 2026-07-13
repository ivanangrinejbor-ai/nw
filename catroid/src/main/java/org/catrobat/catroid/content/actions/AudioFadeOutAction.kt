package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.audio.AudioServiceHolder

class AudioFadeOutAction : TemporalAction() {
    var scope: Scope? = null
    var duration: Formula? = null
    private var startVolume = 0f

    override fun begin() {
        try {
            val dur = duration?.interpretFloat(scope) ?: 1f
            super.setDuration(dur.coerceAtLeast(0f))
            startVolume = AudioServiceHolder.audioService.getVolume()
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed", e)
        }
    }

    override fun update(percent: Float) {
        AudioServiceHolder.audioService.setVolume(startVolume * (1f - percent))
    }

    override fun end() {
        AudioServiceHolder.audioService.setVolume(0f)
    }
}
