package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetAngularDampingAction : TemporalAction() {
    var scope: Scope? = null
    var damping: Formula? = null

    override fun update(percent: Float) {
        val d = damping?.interpretFloat(scope)?.coerceAtLeast(0f) ?: 0f
        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val pw = scene.physicsWorld ?: return
        val sprite = scope?.sprite ?: return
        val po = pw.getPhysicsObject(sprite) ?: return
        po.setAngularDamping(d)
    }
}
