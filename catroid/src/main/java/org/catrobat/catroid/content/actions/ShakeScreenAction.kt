package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

/**
 * Triggers a decaying 2D screen shake on the active stage. The shake is a
 * purely visual camera offset handled by the StageListener render loop.
 */
class ShakeScreenAction : TemporalAction() {
    var scope: Scope? = null
    var intensity: Formula? = null
    var duration: Formula? = null

    private var started = false

    override fun update(percent: Float) {
        // Fire once per run; without this guard a looping script would restart
        // the shake every frame and the offset would never decay.
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
