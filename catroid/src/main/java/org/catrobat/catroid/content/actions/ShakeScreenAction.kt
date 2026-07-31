package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class ShakeScreenAction : TemporalAction() {
    var scope: Scope? = null
    var intensity: Formula? = null
    var duration: Formula? = null

    private var started = false

    override fun update(percent: Float) {
        if (started) return
        started = true

        val listener = StageActivity.getActiveStageListener() ?: return
        val intensityVal = intensity?.interpretDouble(scope)?.toFloat() ?: 0f
        val durationVal = duration?.interpretDouble(scope)?.toFloat() ?: 0f
        if (intensityVal > 0f && durationVal > 0f) {
            listener.startScreenShake(intensityVal, durationVal)
        }
    }

    override fun restart() {
        super.restart()
        started = false
    }
}
