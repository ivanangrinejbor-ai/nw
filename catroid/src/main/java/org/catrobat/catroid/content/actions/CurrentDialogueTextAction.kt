package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.dialogue.DialogueRunnerHolder
import org.catrobat.catroid.formulaeditor.Formula

class CurrentDialogueTextAction : TemporalAction() {
    var scope: Scope? = null
    var variableName: Formula? = null
    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true
        val name = variableName?.interpretString(scope) ?: ""
        val variable = scope?.sprite?.getUserVariable(name)
        val text = DialogueRunnerHolder.runner?.getCurrentText() ?: ""
        variable?.value = text
    }
}
