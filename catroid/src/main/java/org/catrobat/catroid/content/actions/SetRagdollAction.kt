package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetRagdollAction : TemporalAction() {

    var scope: Scope? = null
    var enable: Formula? = null

    override fun update(percent: Float) {
        val sprite = scope?.sprite ?: return
        try {
            val value = enable?.interpretDouble(scope) ?: 0.0
            sprite.ragdollMode = when {
                value >= 2.0 -> 2
                value != 0.0 -> 1
                else -> 0
            }
        } catch (e: Exception) {
            sprite.ragdollMode = 0
        }
    }
}
