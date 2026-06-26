package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity

class CreateObstaclesFromBackgroundAction : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager?.createObstaclesFromBackground()
    }
}
