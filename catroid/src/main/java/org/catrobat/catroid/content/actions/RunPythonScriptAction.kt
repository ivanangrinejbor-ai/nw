package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.UserVarsManager
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.ui.MainMenuActivity

class RunPythonScriptAction : Action() {
    var scope: Scope? = null
    var script: Formula? = null
    var variableName: Formula? = null

    private var hasStarted = false
    private var finished = false

    override fun act(delta: Float): Boolean {
        if (finished) return true
        if (hasStarted) return false
        hasStarted = true

        val scriptContent = script?.interpretString(scope)
        val varName = variableName?.interpretString(scope) ?: "myVar"

        if (scriptContent.isNullOrEmpty()) {
            finished = true
            return true
        }

        val pythonEngine = MainMenuActivity.pythonEngine
        if (pythonEngine == null) {
            Log.e("RunPythonScript", "PythonEngine not available.")
            finished = true
            return true
        }

        pythonEngine.runScriptAsync(scriptContent) { output: String ->
            UserVarsManager.setVar(varName, output)
            finished = true
        }

        return false
    }

    override fun restart() {
        hasStarted = false
        finished = false
        super.restart()
    }
}
