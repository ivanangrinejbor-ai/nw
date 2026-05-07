package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.RenderTextureManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class AddToBufferAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null

    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: "Map"
        val sprite = scope?.sprite
        if (sprite != null) {
            RenderTextureManager.addSpriteToTarget(name, sprite)
        }
    }
}