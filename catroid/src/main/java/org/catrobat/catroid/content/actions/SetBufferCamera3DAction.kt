package org.catrobat.catroid.content.actions
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.RenderTextureManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetBufferCamera3DAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var xForm: Formula? = null; var yForm: Formula? = null; var zForm: Formula? = null
    var yawForm: Formula? = null; var pitchForm: Formula? = null; var rollForm: Formula? = null
    var fovForm: Formula? = null

    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: return
        val x = xForm?.interpretFloat(scope) ?: 0f
        val y = yForm?.interpretFloat(scope) ?: 0f
        val z = zForm?.interpretFloat(scope) ?: 0f
        val yaw = yawForm?.interpretFloat(scope) ?: 0f
        val pitch = pitchForm?.interpretFloat(scope) ?: 0f
        val roll = rollForm?.interpretFloat(scope) ?: 0f
        val fov = fovForm?.interpretFloat(scope) ?: 67f

        RenderTextureManager.setTargetCamera3D(name, x, y, z, yaw, pitch, roll, fov)
    }
}