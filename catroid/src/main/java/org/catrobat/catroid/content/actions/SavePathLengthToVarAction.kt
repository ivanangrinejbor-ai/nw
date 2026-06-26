package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SavePathLengthToVarAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null
    var variableName: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val name = spriteName?.interpretString(s) ?: return
        val varName = variableName?.interpretString(s) ?: return
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        val f = pm.getFollower(name) ?: return
        val waypoints = f.waypoints
        var total = 0.0f
        for (i in 0 until waypoints.size - 1) {
            val a = waypoints[i]
            val b = waypoints[i + 1]
            total += com.badlogic.gdx.math.Vector2.dst(a.x, a.y, b.x, b.y)
        }
        val userVar = s.getUserVariable(varName)
        if (userVar != null) {
            userVar.value = total.toDouble()
        }
    }
}
