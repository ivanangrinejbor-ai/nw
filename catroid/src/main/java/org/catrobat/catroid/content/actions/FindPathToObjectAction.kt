package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class FindPathToObjectAction : TemporalAction() {
    var scope: Scope? = null
    var fromSprite: Formula? = null
    var toSprite: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val from = fromSprite?.interpretString(s) ?: return
        val to = toSprite?.interpretString(s) ?: return
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        val result = pm.findPathToObject(from, to)
        if (result.found) pm.setPathForFollower(from, result.points)
    }
}
