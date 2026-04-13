package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DCreateAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null

    override fun update(percent: Float) {
        val idStr = entityId?.interpretString(scope) ?: return

        val stageListener = StageActivity.activeStageActivity.get()?.stageListener
        stageListener?.fastTwoDManager?.createEntity(idStr)
    }
}