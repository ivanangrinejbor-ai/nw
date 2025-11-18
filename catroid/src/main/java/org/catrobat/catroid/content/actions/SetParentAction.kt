package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetParentAction : TemporalAction() {
    var scope: Scope? = null
    var childObject: Formula? = null
    var parentObject: Formula? = null

    override fun update(percent: Float) {
        val childName = childObject?.interpretString(scope)
        val parentName = parentObject?.interpretString(scope)

        if (childName.isNullOrEmpty() || parentName.isNullOrEmpty()) return

        val sceneManager = StageActivity.getActiveStageListener()?.sceneManager ?: return
        val childGo = sceneManager.findObjectByName(childName)
        val parentGo = sceneManager.findObjectByName(parentName)

        if (childGo != null && parentGo != null) {
            sceneManager.setParent(childGo, parentGo)
        }
    }
}