package org.catrobat.catroid.content.actions
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetAnimationSpeedAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var speed: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.threeDManager ?: return
        val id = objectId?.interpretString(scope) ?: return
        val s = speed?.interpretDouble(scope)?.toFloat() ?: 1f
        manager.setAnimationSpeed(id, s)
    }
}