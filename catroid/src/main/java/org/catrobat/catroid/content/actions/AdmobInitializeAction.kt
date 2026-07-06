package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.admob.AdMobManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity

class AdmobInitializeAction : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val activity = StageActivity.activeStageActivity?.get() ?: return
        AdMobManager.initialize(activity)
    }
}