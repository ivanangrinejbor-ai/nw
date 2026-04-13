package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetAngularVelocityAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var angVel: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        val v = angVel?.interpretFloat(scope)?.toFloat() ?: 0f
        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.setAngularVelocity(id, v)
    }
}