package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class AttachRaySensorAction : TemporalAction() {
    var scope: Scope? = null
    var rayName: Formula? = null
    var objectId: Formula? = null
    var offX: Formula? = null
    var offY: Formula? = null
    var offZ: Formula? = null
    var dirX: Formula? = null
    var dirY: Formula? = null
    var dirZ: Formula? = null
    var dist: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val manager = StageActivity.getActiveStageListener()?.threeDManager ?: return
        val name = rayName?.interpretString(s) ?: return
        val objId = objectId?.interpretString(s) ?: return
        if (name.isEmpty() || objId.isEmpty()) return

        manager.attachRaySensor(
            name, objId,
            offX?.interpretFloat(s) ?: 0f,
            offY?.interpretFloat(s) ?: 0f,
            offZ?.interpretFloat(s) ?: 0f,
            dirX?.interpretFloat(s) ?: 0f,
            dirY?.interpretFloat(s) ?: -1f,
            dirZ?.interpretFloat(s) ?: 0f,
            dist?.interpretFloat(s) ?: 100f
        )
    }
}