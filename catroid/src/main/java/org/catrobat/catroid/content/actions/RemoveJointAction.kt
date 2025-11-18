package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class RemoveJointAction : TemporalAction() {
    var scope: Scope? = null
    var jointName: Formula? = null

    override fun update(percent: Float) {
        val name = jointName?.interpretString(scope)
        if (name.isNullOrEmpty()) return

        val engine = StageActivity.getActiveStageListener()?.threeDManager ?: return
        engine.removeConstraint(name)
    }
}