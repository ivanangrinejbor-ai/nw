package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ApplyTorqueAction : TemporalAction() {
    private var scope: Scope? = null
    private var torque: Formula? = null

    override fun update(percent: Float) {
        val torqueValue: Float
        try {
            torqueValue = torque?.interpretFloat(scope) ?: 0f
        } catch (e: Exception) {
            Log.w(javaClass.simpleName, "Formula interpretation failed", e)
            return
        }

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val pw = scene.physicsWorld ?: return
        val sprite = scope?.sprite ?: return
        if (pw.getPhysicsObject(sprite) == null) return

        pw.applyTorque(sprite, torqueValue)
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setTorque(torque: Formula?) {
        this.torque = torque
    }
}