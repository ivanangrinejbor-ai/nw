package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.notification.NotificationServiceHolder

class RemoveNotificationAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null

    override fun update(percent: Float) {
        val id = notificationId?.interpretInteger(scope) ?: return
        try {
            NotificationServiceHolder.service.remove(id)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to remove notification $id", e)
        }
    }
}
