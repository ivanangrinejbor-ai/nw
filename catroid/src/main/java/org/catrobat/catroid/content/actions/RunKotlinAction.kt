package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.python.PythonEngine

class RunKotlinAction : TemporalAction() {
    var scope: Scope? = null
    var code: Formula? = null

    override fun update(percent: Float) {
        val codeStr = code?.interpretString(scope) ?: return
        try {
            val context = CatroidApplication.getAppContext()
            val engine = PythonEngine(context)
            engine.runScriptAsync(codeStr)
        } catch (_: Exception) {
        }
    }
}