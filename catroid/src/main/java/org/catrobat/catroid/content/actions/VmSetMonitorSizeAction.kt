package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class VmSetMonitorSizeAction : TemporalAction() {
    var scope: Scope? = null
    var widthFormula: Formula? = null
    var heightFormula: Formula? = null

    override fun update(percent: Float) {
        val width = widthFormula?.interpretInteger(scope) ?: 800
        val height = heightFormula?.interpretInteger(scope) ?: 600

        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.stageListener?.resizeVmMonitor(width.toFloat(), height.toFloat())
    }
}