package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetColorAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var r: Formula? = null
    var g: Formula? = null
    var b: Formula? = null
    var a: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        val red = r?.interpretFloat(scope) ?: 255f
        val green = g?.interpretFloat(scope) ?: 255f
        val blue = b?.interpretFloat(scope) ?: 255f
        val alpha = a?.interpretFloat(scope) ?: 100f

        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.setColor(id, red, green, blue, alpha)
    }
}