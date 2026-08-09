package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

/**
 * Action for SetRagdollBrick.
 * Interprets the formula and sets Sprite.isRagdolled accordingly.
 * value != 0.0  →  ragdoll ON
 * value == 0.0  →  ragdoll OFF
 */
class SetRagdollAction : TemporalAction() {

    var scope: Scope? = null
    var enable: Formula? = null

    override fun update(percent: Float) {
        val sprite = scope?.sprite ?: return
        val value = enable?.interpretDouble(scope) ?: 0.0
        sprite.isRagdolled = value != 0.0
    }
}
