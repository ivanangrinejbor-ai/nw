package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CreateNavmeshAction : TemporalAction() {
    var scope: Scope? = null
    var gridWidth: Formula? = null
    var gridHeight: Formula? = null
    var cellSize: Formula? = null

    override fun update(percent: Float) {
        val w = gridWidth?.interpretInteger(scope) ?: 100
        val h = gridHeight?.interpretInteger(scope) ?: 100
        val cs = cellSize?.interpretFloat(scope) ?: 10f
        StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager
            ?.createGrid(w.coerceAtLeast(1), h.coerceAtLeast(1), cs.coerceAtLeast(1f))
    }
}
