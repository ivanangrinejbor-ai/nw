package org.catrobat.catroid.content.actions

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.UserVariable

class WhenVariableChangedAction : LoopAction() {

    var scope: Scope? = null
    var monitoredVariable: UserVariable? = null
    var innerAction: ScriptSequenceAction? = null
    private var lastValue: String? = null
    private var initialized = false

    override fun loopDelegate(delta: Float): Boolean {
        val variable = monitoredVariable ?: return true
        val currentValue = variable.value?.toString() ?: ""

        if (!initialized) {
            lastValue = currentValue
            initialized = true
            return false
        }

        if (currentValue != lastValue) {
            lastValue = currentValue
            val action = innerAction ?: return false
            action.reset()
            scope?.sprite?.let { sprite ->
                sprite.look.addAction(action)
            }
        }
        return false
    }
}
