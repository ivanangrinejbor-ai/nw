package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class AttachRaySensorAction : TemporalAction() {
    lateinit var scope: Scope
    lateinit var rayName: Formula
    lateinit var objectId: Formula
    lateinit var offX: Formula
    lateinit var offY: Formula
    lateinit var offZ: Formula
    lateinit var dirX: Formula
    lateinit var dirY: Formula
    lateinit var dirZ: Formula
    lateinit var dist: Formula

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.threeDManager ?: return

        manager.attachRaySensor(
            rayName.interpretString(scope),
            objectId.interpretString(scope),
            offX.interpretFloat(scope), offY.interpretFloat(scope), offZ.interpretFloat(scope),
            dirX.interpretFloat(scope), dirY.interpretFloat(scope), dirZ.interpretFloat(scope),
            dist.interpretFloat(scope)
        )
    }
}