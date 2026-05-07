package org.catrobat.catroid.content.actions
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.RenderTextureManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetBufferModeAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var render2DFormula: Formula? = null
    var render3DFormula: Formula? = null

    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: return
        val r2d = render2DFormula?.interpretInteger(scope) ?: 1
        val r3d = render3DFormula?.interpretInteger(scope) ?: 0
        RenderTextureManager.setBufferMode(name, r2d > 0, r3d > 0)
    }
}