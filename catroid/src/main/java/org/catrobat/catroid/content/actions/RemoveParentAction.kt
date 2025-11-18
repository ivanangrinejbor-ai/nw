package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class RemoveParentAction : TemporalAction() {
    var scope: Scope? = null
    var childObject: Formula? = null

    override fun update(percent: Float) {
        val childName = childObject?.interpretString(scope)
        if (childName.isNullOrEmpty()) return

        val sceneManager = StageActivity.getActiveStageListener()?.sceneManager ?: return
        val childGo = sceneManager.findObjectByName(childName)

        if (childGo != null) {
            sceneManager.removeParent(childGo)
        }
    }
}