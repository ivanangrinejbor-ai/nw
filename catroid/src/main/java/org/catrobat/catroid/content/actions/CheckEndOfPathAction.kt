package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CheckEndOfPathAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null
    var resultVar: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val name = spriteName?.interpretString(s) ?: return
        val varName = resultVar?.interpretString(s) ?: return
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        val reached = pm.isEndReached(name)
        val userVar = s.getUserVariable(varName)
        if (userVar != null) {
            userVar.value = if (reached) "true" else "false"
        }
    }
}
