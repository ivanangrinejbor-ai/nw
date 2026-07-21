package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.ml.MLBridge

class PtLinearAction() : TemporalAction() {
    var scope: Scope? = null
    var layerNameFormula: Formula? = null
    var inputFormula: Formula? = null
    var outputFormula: Formula? = null
    var inFeaturesFormula: Formula? = null
    var outFeaturesFormula: Formula? = null

    override fun update(percent: Float) {
        val layerName = layerNameFormula?.interpretString(scope) ?: "linear"
        val input = inputFormula?.interpretString(scope) ?: "in"
        val output = outputFormula?.interpretString(scope) ?: "out"
        val inFeatures = (inFeaturesFormula?.interpretFloat(scope) ?: 0.0f).toInt()
        val outFeatures = (outFeaturesFormula?.interpretFloat(scope) ?: 0.0f).toInt()

        if (inFeatures <= 0 || outFeatures <= 0) {
            return
        }

        MLBridge.nativeLayerLinear(layerName, input, output, inFeatures, outFeatures)
    }
}
