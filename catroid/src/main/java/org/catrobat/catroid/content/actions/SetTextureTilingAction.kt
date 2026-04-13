package org.catrobat.catroid.content.actions
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetTextureTilingAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var scaleU: Formula? = null
    var scaleV: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.sceneManager ?: return
        val id = objectId?.interpretString(scope) ?: return
        val go = manager.findGameObject(id) ?: return

        val mat = go.getComponent(org.catrobat.catroid.raptor.MaterialComponent::class.java) ?: return
        mat.uvScaleX = scaleU?.interpretDouble(scope)?.toFloat() ?: 1f
        mat.uvScaleY = scaleV?.interpretDouble(scope)?.toFloat() ?: 1f

        StageActivity.getActiveStageListener()?.threeDManager?.applyPBRMaterial(id, mat)
    }
}