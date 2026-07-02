package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SmoothPathAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null

    override fun update(percent: Float) {
        val name = spriteName?.interpretString(scope) ?: return
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val pathfindingManager = stageListener.pathfindingManager ?: return
        
        val follower = pathfindingManager.getFollower(name) ?: return
        if (follower.waypoints.isNotEmpty()) {
            val sprite = stageListener.spritesFromStage.find { it.name == name }
            val look = sprite?.look
            val w = look?.widthInUserInterfaceDimensionUnit ?: 0f
            val h = look?.heightInUserInterfaceDimensionUnit ?: 0f
            val smoothedPath = pathfindingManager.smoothPath(follower.waypoints, follower.sizeCheckMode, w, h)
            follower.waypoints = smoothedPath
        }
    }
}
