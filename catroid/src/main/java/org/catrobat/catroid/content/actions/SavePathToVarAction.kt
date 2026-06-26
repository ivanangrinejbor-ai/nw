package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SavePathToVarAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null
    var variableName: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val name = spriteName?.interpretString(s) ?: return
        val varName = variableName?.interpretString(s) ?: return
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        val f = pm.getFollower(name) ?: return
        val result = f.waypoints.joinToString(";") { "${it.x},${it.y}" }
        val userVar = s.getUserVariable(varName)
        if (userVar != null) {
            userVar.value = result
        }
    }
}
