package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.ml.MLBridge

class PtCreateTensorAction() : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var shapeFormula: Formula? = null
    var valueFormula: Formula? = null
    var trainableFormula: Formula? = null

    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: "t1"
        val shapeRaw = shapeFormula?.interpretString(scope) ?: "1"

        val shape = shapeRaw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .toIntArray()

        if (shape.isEmpty()) {
            return
        }

        if (valueFormula?.interpretString(scope) == "RANDOM") {
            val isTrainable = (trainableFormula?.interpretFloat(scope) ?: 0.0f) > 0.5f
            MLBridge.nativeCreateRandomTensor(name, shape, isTrainable)
        } else {
            val value = valueFormula?.interpretFloat(scope) ?: 0.0f
            val isTrainable = (trainableFormula?.interpretFloat(scope) ?: 0.0f) > 0.5f
            MLBridge.nativeCreateTensor(name, shape, value, isTrainable)
        }
    }
}