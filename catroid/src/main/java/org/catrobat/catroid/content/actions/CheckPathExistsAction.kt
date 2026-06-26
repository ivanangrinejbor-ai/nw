package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CheckPathExistsAction : TemporalAction() {
    var scope: Scope? = null
    var startX: Formula? = null
    var startY: Formula? = null
    var endX: Formula? = null
    var endY: Formula? = null
    var resultVar: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val sx = startX?.interpretFloat(s) ?: return
        val sy = startY?.interpretFloat(s) ?: return
        val ex = endX?.interpretFloat(s) ?: return
        val ey = endY?.interpretFloat(s) ?: return
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        val path = pm.findPath(sx, sy, ex, ey)
        val exists = path.found && path.points.isNotEmpty()
        val varName = resultVar?.interpretString(s) ?: return
        val userVar = s.getUserVariable(varName)
        if (userVar != null) {
            userVar.value = if (exists) 1.0 else 0.0
        }
    }
}
