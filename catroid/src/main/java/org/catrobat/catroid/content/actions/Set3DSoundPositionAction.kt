package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Set3DSoundPositionAction() : TemporalAction() {
    var scope: Scope? = null
    var soundName: Formula? = null
    var x: Formula? = null
    var y: Formula? = null
    var z: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.getActiveStageListener()?.threeDManager ?: return
        val name = soundName?.interpretString(scope) ?: return
        val posX = x?.interpretFloat(scope) ?: 0f
        val posY = y?.interpretFloat(scope) ?: 0f
        val posZ = z?.interpretFloat(scope) ?: 0f

        threeDManager.updateSoundPosition(name, posX, posY, posZ)
    }
}