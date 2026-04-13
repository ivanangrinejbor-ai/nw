package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetCollisionFilterAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var isSensor: Formula? = null
    var group: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        val sensor = (isSensor?.interpretFloat(scope) ?: 0f) >= 0.5f
        val groupIndex = group?.interpretFloat(scope)?.toInt() ?: 0

        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.setCollisionFilter(id, sensor, groupIndex)
    }
}