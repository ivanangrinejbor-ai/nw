package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CreatePointJointAction : TemporalAction() {
    var scope: Scope? = null
    var constraintName: Formula? = null
    var objectA: Formula? = null
    var objectB: Formula? = null
    var pivotAX: Formula? = null
    var pivotAY: Formula? = null
    var pivotAZ: Formula? = null
    var pivotBX: Formula? = null
    var pivotBY: Formula? = null
    var pivotBZ: Formula? = null

    override fun update(percent: Float) {
        val name = constraintName?.interpretString(scope)
        val nameA = objectA?.interpretString(scope)
        val nameB = objectB?.interpretString(scope)

        if (name.isNullOrEmpty() || nameA.isNullOrEmpty() || nameB.isNullOrEmpty()) return

        val engine = StageActivity.getActiveStageListener()?.threeDManager ?: return

        val pAX = pivotAX?.interpretFloat(scope) ?: 0f
        val pAY = pivotAY?.interpretFloat(scope) ?: 0f
        val pAZ = pivotAZ?.interpretFloat(scope) ?: 0f
        val pBX = pivotBX?.interpretFloat(scope) ?: 0f
        val pBY = pivotBY?.interpretFloat(scope) ?: 0f
        val pBZ = pivotBZ?.interpretFloat(scope) ?: 0f

        val pivotA = Vector3(pAX, pAY, pAZ)
        val pivotB = Vector3(pBX, pBY, pBZ)

        engine.createPoint2PointConstraint(name, nameA, nameB, pivotA, pivotB)
    }
}