package org.catrobat.catroid.codeanalysis

import android.content.Context
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick

class AiSuggestionRule(private val context: Context) : AnalysisRule {
    private var lastAnalysis: Map<Brick, AnalysisResult> = emptyMap()
    var enabled: Boolean = false

    fun reanalyze() {
        if (!enabled || !AiProjectAssistant.isLoaded()) {
            lastAnalysis = emptyMap()
            return
        }
        lastAnalysis = AiProjectAssistant.generateAnalysisResults()
    }

    override fun analyze(brick: Brick): AnalysisResult? {
        return lastAnalysis[brick]
    }

    fun getResults(): Map<Brick, AnalysisResult> = lastAnalysis
}
