package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class AvoidObjectsByColorAction : TemporalAction() {
    var scope: Scope? = null
    var hexColor: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val color = hexColor?.interpretString(s) ?: return
        StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager?.setAvoidColor(color)
    }
}
