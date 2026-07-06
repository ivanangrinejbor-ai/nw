package org.catrobat.catroid.content.actions

import android.content.Intent
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.service.ForegroundService
import org.catrobat.catroid.stage.StageActivity

class StopBackgroundAction : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        try {
            val activity = StageActivity.activeStageActivity?.get() ?: return

            val intent = Intent(activity, ForegroundService::class.java)
            activity.stopService(intent)
            Log.d(javaClass.simpleName, "Foreground service stopped")
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to stop foreground service", e)
        }
    }
}
