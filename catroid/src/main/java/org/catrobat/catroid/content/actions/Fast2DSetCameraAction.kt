package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetCameraAction : TemporalAction() {
    var scope: Scope? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var zoom: Formula? = null

    override fun update(percent: Float) {
        val x = posX?.interpretFloat(scope) ?: 0f
        val y = posY?.interpretFloat(scope) ?: 0f
        val z = zoom?.interpretFloat(scope) ?: 1f
        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.setCamera(x, y, z)
    }
}