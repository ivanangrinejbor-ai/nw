package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

@LunoClass
class SetHingeMotorAction : TemporalAction() {
    var scope: Scope? = null
    var constraintId: Formula? = null
    var targetAngle: Formula? = null
    var maxForce: Formula? = null

    override fun update(percent: Float) {
        val cId = constraintId?.interpretString(scope) ?: "hinge1"
        val angle = targetAngle?.interpretFloat(scope) ?: 0f
        val force = maxForce?.interpretFloat(scope) ?: 10f

        val engine = StageActivity.getActiveStageListener()?.sceneManager?.engine
        engine?.setHingeMotorTarget(cId, angle, force)
    }
}