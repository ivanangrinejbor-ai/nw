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
        val activeScope = scope ?: return
        val idStr = objectId?.interpretString(activeScope) ?: ""
        val pathStr = modelPath?.interpretString(activeScope) ?: ""

        if (idStr.isEmpty()) return

        val threeDManager = StageActivity.getActiveStageListener()?.threeDManager
        if (threeDManager == null) {
            Log.w("Replace3DModel", "ThreeDManager not available")
            return
        }

        val project = activeScope.project ?: return
        val projectFile = project.getFile(pathStr)
        val resolvedPath = if (projectFile != null && projectFile.exists()) {
            projectFile.absolutePath
        } else {
            pathStr
        }
        threeDManager.replaceModel(idStr, resolvedPath)
    }
}
