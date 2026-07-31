package org.catrobat.catroid.codeanalysis

import org.catrobat.catroid.content.bricks.Brick

interface AnalysisRule {
    fun analyze(brick: Brick): AnalysisResult?
}