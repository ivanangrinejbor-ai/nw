package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.dialogue.DialogueRunnerHolder

class CloseDialogueAction : TemporalAction() {
    var scope: Scope? = null
    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true
        DialogueRunnerHolder.runner?.endDialogue()
    }
}
