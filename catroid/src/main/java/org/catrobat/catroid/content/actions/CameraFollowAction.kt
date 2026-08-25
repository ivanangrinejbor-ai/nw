package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CameraFollowAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null
    var smooth: Formula? = null
    var offsetX: Formula? = null
    var offsetY: Formula? = null
    private var started = false

    override fun update(percent: Float) {
        if (started) return
        started = true
        val currentScope = scope ?: return
        val listener = StageActivity.getActiveStageListener() ?: return
        val name = spriteName?.interpretString(currentScope)?.trim().orEmpty()
        try {
            val smoothValue = smooth?.interpretDouble(currentScope)?.toFloat() ?: 0f
            val dx = offsetX?.interpretDouble(currentScope)?.toFloat() ?: 0f
            val dy = offsetY?.interpretDouble(currentScope)?.toFloat() ?: 0f
            listener.setCameraFollow(name, smoothValue, dx, dy)
        } catch (e: Exception) {
        }
    }

    override fun restart() {
        super.restart()
        started = false
    }
}
