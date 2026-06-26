package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class GetNextPathPointAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null
    var varX: Formula? = null
    var varY: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val name = spriteName?.interpretString(s) ?: return
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        val point = pm.getNextPathPoint(name) ?: return

        val varXName = varX?.interpretString(s) ?: return
        val userVarX = s.getUserVariable(varXName)
        if (userVarX != null) {
            userVarX.value = point.x.toDouble()
        }
        val varYName = varY?.interpretString(s) ?: return
        val userVarY = s.getUserVariable(varYName)
        if (userVarY != null) {
            userVarY.value = point.y.toDouble()
        }
    }
}
