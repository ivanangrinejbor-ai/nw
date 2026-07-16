package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class DeleteCloneByNumberAction : TemporalAction() {
    var scope: Scope? = null
    var cloneNumber: Formula? = null

    private var started = false

    override fun restart() {
        started = false
        super.restart()
    }

    override fun update(percent: Float) {
        if (started) return
        val s = scope ?: return
        val number = cloneNumber?.interpretInteger(s) ?: return
        started = true
        val stageListener = StageActivity.getActiveStageListener() ?: return
        stageListener.removeCloneByIndex(number)
    }
}
