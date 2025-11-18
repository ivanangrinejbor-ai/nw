package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CloneObjectAction : TemporalAction() {
    var scope: Scope? = null
    var sourceObjectName: Formula? = null
    var cloneObjectName: Formula? = null

    override fun update(percent: Float) {
        val sourceName = sourceObjectName?.interpretString(scope)
        val cloneName = cloneObjectName?.interpretString(scope)
        if (sourceName.isNullOrEmpty() || cloneName.isNullOrEmpty()) return

        val sceneManager = StageActivity.getActiveStageListener()?.sceneManager ?: return

        val sourceGo = sceneManager.findObjectByName(sourceName) ?: return

        sceneManager.cloneGameObject(sourceGo, cloneName)
    }
}