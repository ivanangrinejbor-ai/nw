package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.JsonStore
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class JsonParseAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var textFormula: Formula? = null

    override fun update(percent: Float) {
        val currentScope = scope ?: return
        val name = nameFormula?.interpretString(currentScope)?.trim().orEmpty()
        val text = textFormula?.interpretString(currentScope).orEmpty()
        if (name.isNotEmpty()) JsonStore.parse(name, text)
    }
}
