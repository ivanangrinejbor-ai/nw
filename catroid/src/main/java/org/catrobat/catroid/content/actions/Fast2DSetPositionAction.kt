package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetPositionAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var posX: Formula? = null
    var posY: Formula? = null

    override fun update(percent: Float) {
        val idStr = entityId?.interpretString(scope) ?: return
        val xVal = posX?.interpretFloat(scope) ?: 0f
        val yVal = posY?.interpretFloat(scope) ?: 0f

        val stageListener = StageActivity.activeStageActivity.get()?.stageListener
        stageListener?.fastTwoDManager?.setPosition(idStr, xVal, yVal)
    }
}