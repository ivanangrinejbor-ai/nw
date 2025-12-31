package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.GameObject
import org.catrobat.catroid.raptor.ParticleComponent
import org.catrobat.catroid.stage.StageActivity

class CreateParticlesAction : TemporalAction() {
    var scope: Scope? = null
    var particleId: Formula? = null
    var looping: Formula? = null
    var duration: Formula? = null
    var startLifetime: Formula? = null
    var startSpeed: Formula? = null
    var startSize: Formula? = null
    var gravityModifier: Formula? = null
    var maxParticles: Formula? = null
    var emissionRate: Formula? = null
    var coneAngle: Formula? = null
    var coneRadius: Formula? = null
    var startColor: Formula? = null
    var endColor: Formula? = null
    var endSize: Formula? = null
    var texturePath: Formula? = null
    var isAdditive: Formula? = null
    var startRotation: Formula? = null
    var rotationOverLifetime: Formula? = null
    var positionX: Formula? = null
    var positionY: Formula? = null
    var positionZ: Formula? = null
    var rotationPitch: Formula? = null // X
    var rotationYaw: Formula? = null   // Y
    var rotationRoll: Formula? = null  // Z

    override fun update(percent: Float) {
        val listener = StageActivity.getActiveStageListener() ?: return
        val sceneManager = listener.sceneManager ?: return
        val id = particleId?.interpretString(scope) ?: "particles"
        Log.d("CreateParticlesAction", "Executing for ID: $id")

        var hostObject = sceneManager.findGameObject(id)
        if (hostObject == null) {
            hostObject = sceneManager.createGameObject(id)
            Log.d("CreateParticlesAction", "Created new GameObject: $id")
        } else {
            hostObject.components.removeIf { it is ParticleComponent }
            Log.d("CreateParticlesAction", "Found existing GameObject, removing old particle component.")
        }

        val component = ParticleComponent()

        component.looping = (looping?.interpretInteger(scope) ?: 1) != 0
        component.duration = duration?.interpretFloat(scope) ?: 5.0f
        component.startLifetime = startLifetime?.interpretFloat(scope) ?: 2.0f
        component.startSpeed = startSpeed?.interpretFloat(scope) ?: 5.0f
        component.startSize = startSize?.interpretFloat(scope) ?: 1.0f
        component.gravityModifier = gravityModifier?.interpretFloat(scope) ?: 0.0f
        component.maxParticles = maxParticles?.interpretInteger(scope) ?: 1000
        component.emissionRate = emissionRate?.interpretFloat(scope) ?: 10f
        component.coneAngle = coneAngle?.interpretFloat(scope) ?: 25.0f
        component.coneRadius = coneRadius?.interpretFloat(scope) ?: 0.1f
        component.endSize = endSize?.interpretFloat(scope) ?: 2.0f
        component.isAdditive = (isAdditive?.interpretInteger(scope) ?: 1) != 0
        component.startRotation = startRotation?.interpretFloat(scope) ?: 0f
        component.rotationOverLifetime = rotationOverLifetime?.interpretFloat(scope) ?: 360f

        val startColorHex = startColor?.interpretString(scope) ?: "#FFFF887F"
        val endColorHex = endColor?.interpretString(scope) ?: "#FF330000"
        component.startColor = parseHexColor(startColorHex)
        component.endColor = parseHexColor(endColorHex)

        val texture = texturePath?.interpretString(scope)
        if (texture != null && texture.isNotEmpty()) {
            component.texturePath = texture
        }

        hostObject.addComponent(component)
        Log.d("CreateParticlesAction", "ParticleComponent added to GameObject.")

        val x = positionX?.interpretFloat(scope)
        val y = positionY?.interpretFloat(scope)
        val z = positionZ?.interpretFloat(scope)

        if (x != null || y != null || z != null) {
            hostObject.transform.position.set(x ?: 0f, y ?: 0f, z ?: 0f)
        } else {
            val spritePosition = scope?.sprite?.look?.let { look ->
                com.badlogic.gdx.math.Vector3(look.x, look.y, 0f)
            } ?: com.badlogic.gdx.math.Vector3.Zero
            hostObject.transform.position.set(spritePosition)
        }

        val pitch = rotationPitch?.interpretFloat(scope) ?: 0f
        val yaw = rotationYaw?.interpretFloat(scope) ?: 0f
        val roll = rotationRoll?.interpretFloat(scope) ?: 0f
        hostObject.transform.rotation.setEulerAngles(yaw, pitch, roll)

        sceneManager.rebuildGameObject(hostObject)
        Log.d("CreateParticlesAction", "rebuildGameObject called for $id. Effect should appear on next frame.")
    }

    private fun parseHexColor(hex: String): com.badlogic.gdx.graphics.Color {
        return try {
            val androidColor = android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
            val r = android.graphics.Color.red(androidColor) / 255f
            val g = android.graphics.Color.green(androidColor) / 255f
            val b = android.graphics.Color.blue(androidColor) / 255f
            val a = android.graphics.Color.alpha(androidColor) / 255f
            com.badlogic.gdx.graphics.Color(r, g, b, a)
        } catch (e: Exception) {
            com.badlogic.gdx.graphics.Color.MAGENTA
        }
    }
}