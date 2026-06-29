package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class CreateParticleSystemAction : TemporalAction() {
    var scope: Scope? = null
    var particleId: Formula? = null
    var maxCount: Formula? = null
    var lifetime: Formula? = null
    var speed: Formula? = null

    override fun update(percent: Float) {
        // Particle system creation placeholder - parameters are stored for future integration
    }
}
