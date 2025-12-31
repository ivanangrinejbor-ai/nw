package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class VmRelativeMouseMoveAction : TemporalAction() {
    var scope: Scope? = null
    var dxFormula: Formula? = null
    var dyFormula: Formula? = null
    var maskFormula: Formula? = null

    override fun update(percent: Float) {
        val dx = dxFormula?.interpretFloat(scope) ?: 0f
        val dy = dyFormula?.interpretFloat(scope) ?: 0f
        val mask = maskFormula?.interpretInteger(scope) ?: 0

        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.moveVmMouseRelative(dx, dy, mask)
    }
}