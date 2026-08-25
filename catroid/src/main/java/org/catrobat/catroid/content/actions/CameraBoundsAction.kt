package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CameraBoundsAction : TemporalAction() {
    var scope: Scope? = null
    var minX: Formula? = null
    var minY: Formula? = null
    var maxX: Formula? = null
    var maxY: Formula? = null
    private var started = false

    override fun update(percent: Float) {
        if (started) return
        started = true
        val currentScope = scope ?: return
        val listener = StageActivity.getActiveStageListener() ?: return
        try {
            val x1 = minX?.interpretDouble(currentScope)?.toFloat() ?: return
            val y1 = minY?.interpretDouble(currentScope)?.toFloat() ?: return
            val x2 = maxX?.interpretDouble(currentScope)?.toFloat() ?: return
            val y2 = maxY?.interpretDouble(currentScope)?.toFloat() ?: return
            listener.setCameraBounds(x1, y1, x2, y2)
        } catch (e: Exception) {
        }
    }

    override fun restart() {
        super.restart()
        started = false
    }
}
