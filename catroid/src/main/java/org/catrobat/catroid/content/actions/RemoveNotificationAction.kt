package org.catrobat.catroid.content.actions

import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class RemoveNotificationAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null

    override fun update(percent: Float) {
        val id = notificationId?.interpretInteger(scope) ?: return
        try {
            val activity = StageActivity.activeStageActivity.get() ?: return
            NotificationManagerCompat.from(activity).cancel(id)
            Log.d(javaClass.simpleName, "Notification $id removed from tray")
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to remove notification $id", e)
        }
    }
}
