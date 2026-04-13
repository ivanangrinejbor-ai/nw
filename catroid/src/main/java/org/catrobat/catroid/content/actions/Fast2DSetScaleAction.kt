package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetScaleAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var sx: Formula? = null
    var sy: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        val x = sx?.interpretFloat(scope)?: 1f
        val y = sy?.interpretFloat(scope)?: 1f
        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.setScale(id, x, y)
    }
}