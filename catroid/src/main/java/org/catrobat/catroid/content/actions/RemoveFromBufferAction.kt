package org.catrobat.catroid.content.actions
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.RenderTextureManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class RemoveFromBufferAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: "Map"
        scope?.sprite?.let { RenderTextureManager.removeSpriteFromTarget(name, it) }
    }
}