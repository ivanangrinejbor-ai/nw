package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class DeleteCloneByNumberAction : TemporalAction() {
    var scope: Scope? = null
    var cloneNumber: Formula? = null

    override fun update(percent: Float) {
        val number = cloneNumber?.interpretInteger(scope) ?: return
        val stageListener = StageActivity.getActiveStageListener() ?: return
        stageListener.removeCloneByIndex(number)
    }
}
