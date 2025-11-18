package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CreateSpringConstraintAction() : TemporalAction() {
    var scope: Scope? = null
    var constraintId: Formula? = null
    var objectIdA: Formula? = null
    var objectIdB: Formula? = null
    var pivotAx: Formula? = null; var pivotAy: Formula? = null; var pivotAz: Formula? = null
    var pivotBx: Formula? = null; var pivotBy: Formula? = null; var pivotBz: Formula? = null

    override fun update(percent: Float) {
        val engine = StageActivity.getActiveStageListener()?.threeDManager ?: return
        val id = constraintId?.interpretString(scope) ?: return
        val idA = objectIdA?.interpretString(scope) ?: return
        val idB = objectIdB?.interpretString(scope) ?: ""

        val pAx = pivotAx?.interpretFloat(scope) ?: 0f
        val pAy = pivotAy?.interpretFloat(scope) ?: 0f
        val pAz = pivotAz?.interpretFloat(scope) ?: 0f

        val pBx = pivotBx?.interpretFloat(scope) ?: 0f
        val pBy = pivotBy?.interpretFloat(scope) ?: 0f
        val pBz = pivotBz?.interpretFloat(scope) ?: 0f

        val frameA = Matrix4().set(Vector3(pAx, pAy, pAz), Quaternion())
        val frameB = Matrix4().set(Vector3(pBx, pBy, pBz), Quaternion())

        engine.createSpringConstraint(id, idA, idB, frameA, frameB)
    }
}