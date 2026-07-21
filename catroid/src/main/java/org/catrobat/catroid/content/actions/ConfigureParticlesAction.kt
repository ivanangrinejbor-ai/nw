package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ConfigureParticlesAction : TemporalAction() {
    var scope: Scope? = null
    var particleId: Formula? = null
    var value: Formula? = null
    var configTypeSelection: Int = 0

    override fun update(percent: Float) {
        val idStr = particleId?.interpretString(scope) ?: ""
        val valStr = value?.interpretString(scope) ?: ""

        if (idStr.isEmpty()) return

        // TODO: implement particle configuration when ThreeDManager particle API is fully available
    }
}
