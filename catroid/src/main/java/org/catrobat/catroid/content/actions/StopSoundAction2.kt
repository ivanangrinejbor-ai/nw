package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class StopSoundAction2 : TemporalAction() {
    var scope: Scope? = null
    var instanceName: Formula? = null

    override fun update(percent: Float) {
        val instance = instanceName?.interpretString(scope)
        if (instance.isNullOrEmpty()) return

        StageActivity.getActiveStageListener()?.threeDManager?.stopSound(instance)
    }
}