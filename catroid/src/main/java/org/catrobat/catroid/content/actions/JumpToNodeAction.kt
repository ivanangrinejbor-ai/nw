package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.dialogue.DialogueRunnerHolder
import org.catrobat.catroid.formulaeditor.Formula

class JumpToNodeAction : TemporalAction() {
    var scope: Scope? = null
    var nodeId: Formula? = null
    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true
        val id = nodeId?.interpretString(scope) ?: return
        DialogueRunnerHolder.runner?.jumpTo(id)
    }
}
