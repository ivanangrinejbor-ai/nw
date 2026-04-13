package org.catrobat.catroid.content.actions
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetSoundInstanceVolumeAction : TemporalAction() {
    var scope: Scope? = null
    var instanceName: Formula? = null
    var volume: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.threeDManager ?: return
        val name = instanceName?.interpretString(scope) ?: return
        val vol = volume?.interpretDouble(scope)?.toFloat() ?: 100f
        manager.setSoundInstanceVolume(name, vol)
    }
}