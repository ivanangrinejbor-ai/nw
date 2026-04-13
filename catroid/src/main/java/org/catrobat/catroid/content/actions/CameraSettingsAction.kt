package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CameraSettingsAction : TemporalAction() {
    var scope: Scope? = null
    var fov: Formula? = null
    var shakeIntensity: Formula? = null
    var shakeDuration: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.threeDManager ?: return

        fov?.let { manager.setCameraFov(it.interpretDouble(scope).toFloat()) }

        val intensity = shakeIntensity?.interpretDouble(scope)?.toFloat() ?: 0f
        val duration = shakeDuration?.interpretDouble(scope)?.toFloat() ?: 0f

        if (intensity > 0) {
            manager.startCameraShake(intensity, duration)
        }
    }
}