package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class EnableDynamicReplanningAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null
    var enabled: Formula? = null

    override fun update(percent: Float) {
        val name = spriteName?.interpretString(scope) ?: return
        val enabledValue = enabled?.interpretString(scope)?.toBoolean() ?: return
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val pathfindingManager = stageListener.pathfindingManager ?: return
        
        pathfindingManager.enableDynamicReplanning(name, enabledValue)
    }
}
