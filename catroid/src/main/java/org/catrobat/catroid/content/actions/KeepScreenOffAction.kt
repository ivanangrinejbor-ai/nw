package org.catrobat.catroid.content.actions

import android.view.WindowManager
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.stage.StageActivity

class KeepScreenOffAction : TemporalAction() {
    override fun update(percent: Float) {
        StageActivity.activeStageActivity.get()?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
