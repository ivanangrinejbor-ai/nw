package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class VideoAction() : TemporalAction() {
    var scope: Scope? = null
    var videoPath: Formula? = null
    var action: Formula? = null
    var seekTime: Formula? = null

    override fun update(percent: Float) {
        val currentScope = scope ?: return
        val activity = StageActivity.activeStageActivity.get() ?: return
        val viewId = videoPath?.interpretString(currentScope) ?: return
        val actionType = action?.interpretInteger(currentScope) ?: 0

        when (actionType) {
            0 -> activity.playVideo(viewId)
            1 -> activity.pauseVideo(viewId)
            2 -> activity.removeView(viewId)
            3 -> activity.seekVideoTo(viewId, seekTime?.interpretInteger(currentScope) ?: 0)
            else -> Log.w("VideoAction", "Unknown action type: $actionType")
        }
    }

    override fun reset() {
        super.reset()
        scope = null
        videoPath = null
        action = null
        seekTime = null
    }
}
