package org.catrobat.catroid.content.actions
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.RenderTextureManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SaveBufferAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var fileFormula: Formula? = null
    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: "Map"
        val file = fileFormula?.interpretString(scope) ?: "Screenshot"
        RenderTextureManager.saveBufferToFile(name, file)
    }
}