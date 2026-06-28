package org.catrobat.catroid.content.actions

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
        val x = forceX?.interpretFloat(scope) ?: 0f
        val y = forceY?.interpretFloat(scope) ?: 0f

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val sprite = scope?.sprite ?: return

        scene.physicsWorld.applyForce(sprite, Vector2(x, y), Vector2(sprite.look.x, sprite.look.y))
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