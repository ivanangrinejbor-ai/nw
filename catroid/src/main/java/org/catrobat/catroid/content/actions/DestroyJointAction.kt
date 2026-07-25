package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class DestroyJointAction : TemporalAction() {
    var scope: Scope? = null
    var jointId: Formula? = null

    override fun update(percent: Float) {
        val id = jointId?.interpretString(scope) ?: return
        if (id.isEmpty()) return
        val pw = ProjectManager.getInstance().currentlyPlayingScene?.physicsWorld ?: return
        pw.destroyJoint(id)
    }
}