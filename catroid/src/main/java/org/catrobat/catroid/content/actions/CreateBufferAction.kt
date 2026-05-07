package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.RenderTextureManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class CreateBufferAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var widthFormula: Formula? = null
    var heightFormula: Formula? = null

    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: "Map"
        val w = widthFormula?.interpretInteger(scope) ?: 512
        val h = heightFormula?.interpretInteger(scope) ?: 512
        RenderTextureManager.createRenderTarget(name, w, h)
    }
}