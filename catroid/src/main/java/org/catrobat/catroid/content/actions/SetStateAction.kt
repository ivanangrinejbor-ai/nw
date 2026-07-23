package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.StateMachineManager
import org.catrobat.catroid.formulaeditor.Formula

class SetStateAction : TemporalAction() {
    var scope: Scope? = null
    var machineFormula: Formula? = null
    var stateFormula: Formula? = null

    override fun update(percent: Float) {
        val currentScope = scope ?: return
        val machine = machineFormula?.interpretString(currentScope) ?: ""
        val state = stateFormula?.interpretString(currentScope) ?: ""
        StateMachineManager.setState(currentScope.sprite, machine, state)
    }
}
