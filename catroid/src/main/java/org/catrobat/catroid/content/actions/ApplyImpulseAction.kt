package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ApplyImpulseAction : TemporalAction() {
    private var scope: Scope? = null
    private var impulseX: Formula? = null
    private var impulseY: Formula? = null

    override fun update(percent: Float) {
        val x = impulseX?.interpretFloat(scope) ?: 0f
        val y = impulseY?.interpretFloat(scope) ?: 0f

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val sprite = scope?.sprite ?: return

        // Pass the body-local center (0,0): PhysicsWorld.applyImpulse already converts the
        // point to a world point via Body.getWorldPoint, so passing the sprite's world
        // position would double-transform it. Using the local center yields the single,
        // correct world application point (the sprite's center).
        scene.physicsWorld.applyImpulse(sprite, Vector2(x, y), Vector2(0f, 0f))
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setImpulseX(impulseX: Formula?) {
        this.impulseX = impulseX
    }

    fun setImpulseY(impulseY: Formula?) {
        this.impulseY = impulseY
    }
}