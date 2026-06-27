package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class MoveToObjectAction : TemporalAction() {
    var scope: Scope? = null
    var targetObject: Formula? = null
    var speed: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val currentName = s.sprite.name
        val targetName = targetObject?.interpretString(s) ?: return
        if (targetName.isEmpty() || targetName == currentName) return
        val spd = speed?.interpretFloat(s) ?: 1f
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        if (pm.navGrid == null) {
            pm.createGrid(30, 20, 32f)
        }
        val result = pm.findPathToObject(currentName, targetName)
        if (result.found) {
            pm.setPathForFollower(currentName, result.points)
            pm.startFollowing(currentName, spd)
        }
    }
}
