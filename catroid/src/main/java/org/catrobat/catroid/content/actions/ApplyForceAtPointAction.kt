package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ApplyForceAtPointAction : TemporalAction() {
    var scope: Scope? = null
    var forceX: Formula? = null
    var forceY: Formula? = null
    var pointX: Formula? = null
    var pointY: Formula? = null

    override fun update(percent: Float) {
        val fx: Float
        val fy: Float
        val px: Float
        val py: Float
        try {
            fx = forceX?.interpretFloat(scope) ?: 0f
            fy = forceY?.interpretFloat(scope) ?: 0f
            px = pointX?.interpretFloat(scope) ?: 0f
            py = pointY?.interpretFloat(scope) ?: 0f
        } catch (e: Exception) {
            Log.w(javaClass.simpleName, "Formula interpretation failed", e)
            return
        }
        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val pw = scene.physicsWorld ?: return
        val sprite = scope?.sprite ?: return
        if (pw.getPhysicsObject(sprite) == null) return

        pw.applyForce(sprite, Vector2(fx, fy), Vector2(px, py))
    }
}
