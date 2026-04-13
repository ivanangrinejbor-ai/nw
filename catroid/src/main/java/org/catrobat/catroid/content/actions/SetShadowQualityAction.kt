package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetShadowQualityAction : TemporalAction() {
    var scope: Scope? = null
    var sizeFormula: Formula? = null
    var resolutionFormula: Formula? = null

    override fun update(percent: Float) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val threeDManager = stageListener.threeDManager ?: return

        val size = sizeFormula?.interpretFloat(scope) ?: 100f
        val resolution = resolutionFormula?.interpretFloat(scope)?.toInt() ?: 2048

        val finalSize = size.coerceIn(1f, 2000f)
        val finalRes = resolution.coerceIn(128, 4096)

        threeDManager.setShadowSettings(finalSize, finalRes)
    }
}