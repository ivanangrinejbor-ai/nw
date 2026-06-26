package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class FindPathToXYAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null
    var startX: Formula? = null
    var startY: Formula? = null
    var endX: Formula? = null
    var endY: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val name = spriteName?.interpretString(s) ?: return
        val sx = startX?.interpretFloat(s) ?: return
        val sy = startY?.interpretFloat(s) ?: return
        val ex = endX?.interpretFloat(s) ?: return
        val ey = endY?.interpretFloat(s) ?: return
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        val result = pm.findPath(sx, sy, ex, ey)
        if (result.found) {
            pm.setPathForFollower(name, result.points)
        }
    }
}
