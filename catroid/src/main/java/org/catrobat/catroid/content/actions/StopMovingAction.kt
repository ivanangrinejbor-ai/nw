package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity

class StopMovingAction : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val pm = StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager ?: return
        pm.stopFollowing(s.sprite.name)
    }
}
