package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class CreateGearJointAction : TemporalAction() {
    private var scope: Scope? = null
    private var jointId: Formula? = null
    private var jointAId: Formula? = null
    private var jointBId: Formula? = null
    private var ratio: Formula? = null

    override fun update(percent: Float) {
        val id = jointId?.interpretString(scope) ?: return
        val idA = jointAId?.interpretString(scope) ?: return
        val idB = jointBId?.interpretString(scope) ?: return
        if (id.isEmpty() || idA.isEmpty() || idB.isEmpty()) return

        val r = ratio?.interpretFloat(scope) ?: 1f

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return

        scene.physicsWorld.createGearJoint(id, idA, idB, r)
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setJointId(jointId: Formula?) {
        this.jointId = jointId
    }

    fun setJointAId(jointAId: Formula?) {
        this.jointAId = jointAId
    }

    fun setJointBId(jointBId: Formula?) {
        this.jointBId = jointBId
    }

    fun setRatio(ratio: Formula?) {
        this.ratio = ratio
    }
}