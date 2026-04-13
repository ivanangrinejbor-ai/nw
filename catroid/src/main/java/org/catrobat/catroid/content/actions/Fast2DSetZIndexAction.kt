package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetZIndexAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var zIndex: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        val z = zIndex?.interpretFloat(scope) ?: 0f
        StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.setZIndex(id, z)
    }
}