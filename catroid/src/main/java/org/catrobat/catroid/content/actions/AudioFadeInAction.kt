package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.io.SoundManager

class AudioFadeInAction : TemporalAction() {
    var scope: Scope? = null
    var duration: Formula? = null

    override fun begin() {
        try {
            val dur = duration?.interpretFloat(scope) ?: 1f
            super.setDuration(dur.coerceAtLeast(0f))
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed", e)
        }
    }

    override fun update(percent: Float) {
        val target = SoundManager.getInstance().volume
        SoundManager.getInstance().volume = target * percent
    }

    override fun end() {
        SoundManager.getInstance().volume = SoundManager.getInstance().volume
    }
}
