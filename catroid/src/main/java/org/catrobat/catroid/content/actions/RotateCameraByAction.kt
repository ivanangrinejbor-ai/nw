package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class RotateCameraByAction : TemporalAction() {
    var scope: Scope? = null
    var pitch: Formula? = null
    var yaw: Formula? = null
    var roll: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.threeDManager ?: return

        val p = pitch?.interpretDouble(scope)?.toFloat() ?: 0f
        val y = yaw?.interpretDouble(scope)?.toFloat() ?: 0f
        val r = roll?.interpretDouble(scope)?.toFloat() ?: 0f

        manager.rotateCameraRelative(p, y, r)
    }
}