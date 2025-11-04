package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.virtualmachine.VirtualMachineManager
import java.util.regex.Pattern

class SendVmInputAction : TemporalAction() {
    var scope: Scope? = null
    var inputText: Formula? = null

    private fun mapSpecialKeys(input: String): String {
        val ctrlPattern = Pattern.compile("<CTRL\\+([A-Z])>")
        val matcher = ctrlPattern.matcher(input.uppercase())
        if (matcher.matches()) {
            val letter = matcher.group(1)!![0]
            val ctrlCode = letter.code - 'A'.code + 1
            return ctrlCode.toChar().toString()
        }

        return when (input.uppercase()) {
            "<CTRL+C>" -> 3.toChar().toString()
            "<CTRL+D>" -> 4.toChar().toString()
            "<CTRL+Z>" -> 26.toChar().toString()

            "<ESC>" -> 27.toChar().toString()
            "<TAB>" -> "\t"
            "<ENTER>" -> "\n"
            "<BACKSPACE>" -> 8.toChar().toString()

            "<UP>" -> "${27.toChar()}[A"
            "<DOWN>" -> "${27.toChar()}[B"
            "<RIGHT>" -> "${27.toChar()}[C"
            "<LEFT>" -> "${27.toChar()}[D"

            else -> input
        }
    }

    override fun update(percent: Float) {
        val name = StageActivity.DEFAULT_VM_NAME
        val rawText = inputText?.interpretString(scope) ?: return

        val finalText = mapSpecialKeys(rawText)

        VirtualMachineManager.sendInputToVM(name, finalText)
    }
}