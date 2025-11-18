package org.catrobat.catroid.content.actions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class LoadSceneAdditiveAction() : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null

    override fun update(percent: Float) {
        val sceneManager = StageActivity.getActiveStageListener()?.sceneManager ?: return
        val path = fileName?.interpretString(scope) ?: return
        val projectFile = scope?.project?.getFile(path)

        if (projectFile == null || !projectFile.exists()) {
            return
        }

        val fileHandle = Gdx.files.absolute(projectFile.absolutePath)
        sceneManager.loadAndAddScene(fileHandle)
    }
}