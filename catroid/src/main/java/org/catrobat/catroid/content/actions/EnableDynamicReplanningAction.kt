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
        val strVal = enabled?.interpretString(scope) ?: ""
        val enabledValue = strVal.equals("true", ignoreCase = true) || strVal == "1" || strVal == "1.0" || (strVal.toDoubleOrNull() ?: 0.0) > 0.5
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val pathfindingManager = stageListener.pathfindingManager ?: return
        
        pathfindingManager.enableDynamicReplanning(name, enabledValue)
    }
}
