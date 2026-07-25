package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetBulletAction : TemporalAction() {
    var scope: Scope? = null
    var bullet: Formula? = null

    override fun update(percent: Float) {
        val isBullet = (bullet?.interpretFloat(scope) ?: 1f) >= 0.5f
        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val pw = scene.physicsWorld ?: return
        val sprite = scope?.sprite ?: return
        val po = pw.getPhysicsObject(sprite) ?: return
        po.setBullet(isBullet)
    }
}
