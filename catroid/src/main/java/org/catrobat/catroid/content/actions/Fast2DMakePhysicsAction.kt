package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DMakePhysicsAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var isDyn: Formula? = null
    var shape: Formula? = null
    var den: Formula? = null
    var fric: Formula? = null
    var bnc: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        val isDynamic = (isDyn?.interpretFloat(scope) ?: 1f) >= 0.5f
        val shapeStr = shape?.interpretString(scope) ?: "BOX"
        val density = den?.interpretFloat(scope)?.toFloat() ?: 1f
        val friction = fric?.interpretFloat(scope)?.toFloat() ?: 0.5f
        val bounce = bnc?.interpretFloat(scope)?.toFloat() ?: 0f

        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.makePhysicsBody(id, isDynamic, shapeStr, density, friction, bounce)
    }
}