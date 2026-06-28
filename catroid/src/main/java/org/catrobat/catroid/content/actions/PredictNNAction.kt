package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.NN.OnnxSessionManager

import org.catrobat.catroid.R
import org.catrobat.catroid.content.FloatArrayManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.utils.ToastUtil
import java.util.concurrent.Future

class PredictNNAction() : TemporalAction() {
    var scope: Scope? = null
    var input: Formula? = null
    var variable: UserVariable? = null

    private var hasStarted = false
    private var isFinished = false

    override fun act(delta: Float): Boolean {
        if (!OnnxSessionManager.isWorking) return true
        if (isFinished) return true
        if (hasStarted) return false
        hasStarted = true

        val arrayName = input?.interpretString(scope)
        val safeVariable = variable

        if (arrayName.isNullOrBlank() || safeVariable == null) {
            isFinished = true
            return true
        }

        val inputArray = FloatArrayManager.getArray(arrayName)

        if (inputArray != null && inputArray.isNotEmpty()) {
            OnnxSessionManager.predict(inputArray) { rawResult ->
                if (rawResult != null) {
                    val bestIndex = FloatArrayManager.findMaxIndex(rawResult)
                    val resultString = rawResult.joinToString(",")
                    safeVariable.value = "$bestIndex\n$resultString"
                } else {
                    safeVariable.value = "ERROR"
                }
                isFinished = true
            }
        } else {
            safeVariable.value = "ARRAY_ERROR"
            isFinished = true
        }

        return isFinished
    }

    override fun update(percent: Float) {
        // NN prediction runs asynchronously in act() — no per-frame update needed
    }

    override fun restart() {
        super.restart()
        hasStarted = false
    }
}