package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class PrepareSoundAction2 : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null
    var soundName: Formula? = null

    override fun update(percent: Float) {
        val file = fileName?.interpretString(scope)
        val name = soundName?.interpretString(scope)
        if (file.isNullOrEmpty() || name.isNullOrEmpty()) return

        StageActivity.getActiveStageListener()?.threeDManager?.prepareAudio(name, file, false)
    }
}