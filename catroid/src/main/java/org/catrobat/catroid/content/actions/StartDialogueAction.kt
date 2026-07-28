package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.dialogue.DialogueRunner
import org.catrobat.catroid.dialogue.DialogueRunnerHolder
import org.catrobat.catroid.dialogue.DialogueSerializer
import org.catrobat.catroid.dialogue.DialogueTree
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class StartDialogueAction : TemporalAction() {
    var scope: Scope? = null
    var filePath: Formula? = null
    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true
        val path = filePath?.interpretString(scope) ?: return
        try {
            val file = File(path)
            if (!file.exists()) return
            val tree = DialogueTree.fromJson(file.readText())
            val runner = DialogueRunnerHolder.runner
            runner?.load(tree)
            runner?.start()
        } catch (_: Exception) { }
    }
}
