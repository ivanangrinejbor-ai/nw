package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.ParticleSystem3DComponent
import org.catrobat.catroid.stage.StageActivity

class CreateParticleSystemAction : TemporalAction() {
    var scope: Scope? = null
    var particleId: Formula? = null
    var maxCount: Formula? = null
    var lifetime: Formula? = null
    var speed: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val id = particleId?.interpretString(scope) ?: return
        if (id.isEmpty()) return

        val stageListener = StageActivity.getActiveStageListener()
        if (stageListener == null) {
            Log.w("CreateParticleSystem", "StageListener not available")
            return
        }
        val threeDManager = stageListener.threeDManager
        if (threeDManager == null) {
            Log.w("CreateParticleSystem", "ThreeDManager not available")
            return
        }

        val maxP = maxCount?.interpretInteger(scope) ?: 100
        val life = lifetime?.interpretFloat(scope) ?: 5f
        val spd = speed?.interpretFloat(scope) ?: 5f

        val component = ParticleSystem3DComponent()
        component.maxParticles = maxP
        component.startLifetime = ParticleSystem3DComponent.MinMaxCurve(life)
        component.startSpeed = ParticleSystem3DComponent.MinMaxCurve(spd)
        component.duration = life
        component.looping = true
        component.emission.enabled = true
        component.emission.rateOverTime = ParticleSystem3DComponent.MinMaxCurve(10f)

        val sprite = scope?.sprite
        val transform = Matrix4()
        if (sprite?.look != null) {
            transform.setToTranslation(sprite.look.x, sprite.look.y, 0f)
        }

        threeDManager.updateParticleEffect3D(id, component, transform)
        Log.d("CreateParticleSystem", "Created 3D particle system '$id' maxParticles=$maxP lifetime=${life}s speed=$spd")
    }
}
