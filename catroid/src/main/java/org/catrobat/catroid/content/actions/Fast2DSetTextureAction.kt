package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Fast2DSetTextureAction : TemporalAction() {
    var scope: Scope? = null
    var entityId: Formula? = null
    var fileName: Formula? = null

    override fun update(percent: Float) {
        val id = entityId?.interpretString(scope) ?: return
        val fileStr = fileName?.interpretString(scope) ?: return

        val file = scope?.project?.getFile(fileStr)
        if (file != null && file.exists()) {
            StageActivity.activeStageActivity.get()?.stageListener?.fastTwoDManager?.setTexture(id, file.absolutePath)
        }
    }
}