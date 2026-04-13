package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CameraTouchControlAction : TemporalAction() {
    var scope: Scope? = null
    var enabled: Formula? = null
    var sensitivity: Formula? = null
    var blockUI: Boolean = true
    var areaX: Formula? = null
    var areaY: Formula? = null
    var areaW: Formula? = null
    var areaH: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.threeDManager ?: return

        manager.configureTouchRotation(
            (enabled?.interpretDouble(scope) ?: 1.0) > 0.5,
            sensitivity?.interpretDouble(scope)?.toFloat() ?: 0.2f,
            blockUI,
            areaX?.interpretDouble(scope)?.toFloat() ?: 0f,
            areaY?.interpretDouble(scope)?.toFloat() ?: 0f,
            areaW?.interpretDouble(scope)?.toFloat() ?: 100f,
            areaH?.interpretDouble(scope)?.toFloat() ?: 100f
        )
    }
}