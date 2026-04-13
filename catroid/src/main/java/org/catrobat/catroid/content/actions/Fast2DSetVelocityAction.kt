package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetVelocityAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var vx: Formula? = null
    var vy: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        val x = vx?.interpretFloat(scope)?.toFloat() ?: 0f
        val y = vy?.interpretFloat(scope)?.toFloat() ?: 0f
        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.setVelocity(id, x, y)
    }
}