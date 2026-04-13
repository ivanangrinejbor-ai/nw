package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DDeleteAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.destroyEntity(id)
    }
}