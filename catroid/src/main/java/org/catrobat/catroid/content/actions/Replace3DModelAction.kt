package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Replace3DModelAction() : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var modelPath: Formula? = null

    override fun update(percent: Float) {
        val idStr = objectId?.interpretString(scope) ?: ""
        val pathStr = modelPath?.interpretString(scope) ?: ""

        if (idStr.isEmpty()) return

        val threeDManager = StageActivity.getActiveStageListener()?.threeDManager
        if (threeDManager == null) {
            Log.w("Replace3DModel", "ThreeDManager not available")
            return
        }

        threeDManager.replaceModel(idStr, pathStr)
    }
}
