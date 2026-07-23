package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class ConfigureParticlesAction : TemporalAction() {
    var scope: Scope? = null
    var particleId: Formula? = null
    var value: Formula? = null
    var configTypeSelection: Int = 0

    override fun update(percent: Float) {
        if (scope == null) return
        val idStr = particleId?.interpretString(scope) ?: return
        val valStr = value?.interpretString(scope) ?: return

        if (idStr.isEmpty()) return

        val threeDManager = StageActivity.getActiveStageListener()?.threeDManager
        if (threeDManager == null) {
            Log.w("ConfigureParticles", "ThreeDManager not available")
            return
        }

        val floatVal = valStr.toFloatOrNull() ?: return

        when (configTypeSelection) {
            0 -> threeDManager.setParticleEmissionRate(idStr, floatVal)
            else -> Log.w("ConfigureParticles", "Unknown configTypeSelection: $configTypeSelection")
        }
    }
}
