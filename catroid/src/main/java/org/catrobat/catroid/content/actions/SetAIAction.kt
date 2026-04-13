package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.AIComponent
import org.catrobat.catroid.stage.StageActivity

class SetAIAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var mode: Formula? = null
    var targetId: Formula? = null
    var speed: Formula? = null
    var stopDist: Formula? = null
    var range: Formula? = null
    var step: Formula? = null
    var avoid: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.threeDManager ?: return
        val id = objectId?.interpretString(scope) ?: return

        val ai = AIComponent()
        val m = mode?.interpretDouble(scope)?.toInt() ?: 0
        ai.mode = when(m) {
            1 -> AIComponent.Mode.FOLLOW
            2 -> AIComponent.Mode.MOVE_TO
            else -> AIComponent.Mode.OFF
        }

        ai.targetId = targetId?.interpretString(scope) ?: ""
        ai.speed = speed?.interpretDouble(scope)?.toFloat() ?: 5f
        ai.stopDistance = stopDist?.interpretDouble(scope)?.toFloat() ?: 1.2f
        ai.detectionRange = range?.interpretDouble(scope)?.toFloat() ?: 5f
        ai.stepHeight = step?.interpretDouble(scope)?.toFloat() ?: 0.7f
        ai.avoidObstacles = (avoid?.interpretDouble(scope) ?: 1.0) > 0.5

        manager.setAI(id, ai)
    }
}