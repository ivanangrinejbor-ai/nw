package org.catrobat.catroid.content.actions

import android.util.Log
import android.view.WindowManager
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.stage.StageActivity

class ScreenBrightnessAction : TemporalAction() {
    var scope: Scope? = null
    var brightness: Formula? = null

    override fun update(percent: Float) {
        try {
            val value = brightness?.interpretFloat(scope) ?: 0.5f
            val clamped = value.coerceIn(0.0f, 1.0f)
            val activity = StageActivity.activeStageActivity.get() ?: return
            val lp = activity.window.attributes
            lp.screenBrightness = clamped
            activity.window.attributes = lp
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed", e)
        }
    }
}
