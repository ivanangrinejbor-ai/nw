package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetAngularVelocityAction : TemporalAction() {
    var scope: Scope? = null
    var angularVelocity: Formula? = null

    override fun update(percent: Float) {
        val omega: Float
        try {
            omega = angularVelocity?.interpretFloat(scope) ?: 0f
        } catch (e: Exception) {
            Log.w(javaClass.simpleName, "Formula interpretation failed", e)
            return
        }
        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val pw = scene.physicsWorld ?: return
        val sprite = scope?.sprite ?: return
        val po = pw.getPhysicsObject(sprite) ?: return
        po.setAngularVelocity(Math.toRadians(omega.toDouble()).toFloat())
    }
}
