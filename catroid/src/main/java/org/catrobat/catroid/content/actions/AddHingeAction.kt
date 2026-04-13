package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

@LunoClass
class AddHingeAction : TemporalAction() {
    var scope: Scope? = null
    var constraintId: Formula? = null
    var objectA: Formula? = null
    var objectB: Formula? = null

    var pivotAX: Formula? = null; var pivotAY: Formula? = null; var pivotAZ: Formula? = null
    var axisAX: Formula? = null; var axisAY: Formula? = null; var axisAZ: Formula? = null
    var pivotBX: Formula? = null; var pivotBY: Formula? = null; var pivotBZ: Formula? = null
    var axisBX: Formula? = null; var axisBY: Formula? = null; var axisBZ: Formula? = null

    override fun update(percent: Float) {
        val cId = constraintId?.interpretString(scope) ?: "hinge1"
        val objA = objectA?.interpretString(scope) ?: ""
        val objB = objectB?.interpretString(scope) ?: ""

        val pAx = pivotAX?.interpretFloat(scope) ?: 0f
        val pAy = pivotAY?.interpretFloat(scope) ?: 0f
        val pAz = pivotAZ?.interpretFloat(scope) ?: 0f

        val aAx = axisAX?.interpretFloat(scope) ?: 0f
        val aAy = axisAY?.interpretFloat(scope) ?: 1f
        val aAz = axisAZ?.interpretFloat(scope) ?: 0f

        val pBx = pivotBX?.interpretFloat(scope) ?: 0f
        val pBy = pivotBY?.interpretFloat(scope) ?: 0f
        val pBz = pivotBZ?.interpretFloat(scope) ?: 0f

        val aBx = axisBX?.interpretFloat(scope) ?: 0f
        val aBy = axisBY?.interpretFloat(scope) ?: 1f
        val aBz = axisBZ?.interpretFloat(scope) ?: 0f

        val engine = StageActivity.getActiveStageListener()?.sceneManager?.engine
        if (engine != null && objA.isNotEmpty() && objB.isNotEmpty()) {
            engine.createHingeConstraint(
                cId, objA, objB,
                Vector3(pAx, pAy, pAz), Vector3(aAx, aAy, aAz),
                Vector3(pBx, pBy, pBz), Vector3(aBx, aBy, aBz)
            )
        }
    }
}