package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetBufferOnlyAction : TemporalAction() {
    var scope: Scope? = null
    var stateFormula: Formula? = null

    override fun update(percent: Float) {
        val state = stateFormula?.interpretInteger(scope) ?: 1
        scope?.sprite?.look?.drawOnlyInBuffer = (state > 0)
    }
}