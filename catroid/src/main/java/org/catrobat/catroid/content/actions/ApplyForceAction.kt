package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ApplyForceAction : TemporalAction() {
    private var scope: Scope? = null
    private var forceX: Formula? = null
    private var forceY: Formula? = null

    override fun update(percent: Float) {
        val x: Float
        val y: Float
        try {
            x = forceX?.interpretFloat(scope) ?: 0f
            y = forceY?.interpretFloat(scope) ?: 0f
        } catch (e: Exception) {
            Log.w(javaClass.simpleName, "Formula interpretation failed", e)
            return
        }

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val sprite = scope?.sprite ?: return

        // Pass the body-local center (0,0): PhysicsWorld.applyForce already converts the
        // point to a world point via Body.getWorldPoint, so passing the sprite's world
        // position would double-transform it. Using the local center yields the single,
        // correct world application point (the sprite's center).
        scene.physicsWorld.applyForce(sprite, Vector2(x, y), Vector2(0f, 0f))
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setForceX(forceX: Formula?) {
        this.forceX = forceX
    }

    fun setForceY(forceY: Formula?) {
        this.forceY = forceY
    }
}