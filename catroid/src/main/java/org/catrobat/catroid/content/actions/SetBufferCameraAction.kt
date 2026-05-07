package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.RenderTextureManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetBufferCameraAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var xFormula: Formula? = null
    var yFormula: Formula? = null
    var zoomFormula: Formula? = null
    var rotFormula: Formula? = null

    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: "Map"
        val x = xFormula?.interpretFloat(scope) ?: 0f
        val y = yFormula?.interpretFloat(scope) ?: 0f
        val zoom = zoomFormula?.interpretFloat(scope) ?: 1f
        val rot = rotFormula?.interpretFloat(scope) ?: 0f
        RenderTextureManager.setTargetCamera2D(name, x, y, zoom, rot)
    }
}