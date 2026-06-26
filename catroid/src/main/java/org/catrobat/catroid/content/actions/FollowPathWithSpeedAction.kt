package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class FollowPathWithSpeedAction : TemporalAction() {
    var scope: Scope? = null
    var spriteName: Formula? = null
    var speed: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val name = spriteName?.interpretString(s) ?: return
        val spd = speed?.interpretFloat(s) ?: return
        StageActivity.activeStageActivity.get()?.stageListener?.pathfindingManager?.startFollowing(name, spd)
    }
}
